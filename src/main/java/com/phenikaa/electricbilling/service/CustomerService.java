package com.phenikaa.electricbilling.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phenikaa.electricbilling.domain.AppUser;
import com.phenikaa.electricbilling.domain.Customer;
import com.phenikaa.electricbilling.domain.CustomerStatus;
import com.phenikaa.electricbilling.domain.Role;
import com.phenikaa.electricbilling.dto.CustomerEditForm;
import com.phenikaa.electricbilling.dto.PasswordForm;
import com.phenikaa.electricbilling.dto.ProfileForm;
import com.phenikaa.electricbilling.dto.RegisterForm;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.exception.NotFoundException;
import com.phenikaa.electricbilling.repository.AppUserRepository;
import com.phenikaa.electricbilling.repository.CustomerRepository;
import com.phenikaa.electricbilling.repository.MeterReadingRepository;

import lombok.RequiredArgsConstructor;

/** Đăng ký, hồ sơ cá nhân, quản lý hộ dùng điện (UC01, UC08, UC13). */
@Service
@RequiredArgsConstructor
public class CustomerService {

	private final AppUserRepository userRepository;
	private final CustomerRepository customerRepository;
	private final MeterReadingRepository readingRepository;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;

	/** UC01: tạo tài khoản CUSTOMER và hồ sơ hộ dùng điện. */
	@Transactional
	public Customer register(RegisterForm form) {
		if (!form.getPassword().equals(form.getConfirmPassword())) {
			throw new BusinessException("confirmPassword", "Mật khẩu xác nhận không khớp");
		}
		String username = form.getUsername().trim().toLowerCase();
		String email = form.getEmail().trim().toLowerCase();
		String meter = form.getMeterNumber().trim().toUpperCase();

		if (userRepository.existsByUsername(username)) {
			throw new BusinessException("username", "Tên đăng nhập đã tồn tại");
		}
		if (customerRepository.existsByEmail(email)) {
			throw new BusinessException("email", "Email đã được sử dụng");
		}
		if (customerRepository.existsByMeterNumber(meter)) {
			throw new BusinessException("meterNumber", "Số công tơ đã được đăng ký");
		}

		AppUser user = userRepository.save(
				new AppUser(username, passwordEncoder.encode(form.getPassword()), Role.CUSTOMER));

		Customer c = new Customer();
		c.setUser(user);
		c.setCustomerCode("TMP-" + UUID.randomUUID().toString().substring(0, 15)); // thay bằng mã thật sau khi có id
		c.setFullName(form.getFullName().trim());
		c.setAddress(form.getAddress().trim());
		c.setPhone(form.getPhone().trim());
		c.setEmail(email);
		c.setMeterNumber(meter);
		c.setInitialReading(form.getInitialReading());
		c.setStatus(CustomerStatus.ACTIVE);
		c.setCreatedAt(LocalDateTime.now(clock));
		c = customerRepository.save(c);
		c.setCustomerCode(String.format("KH%06d", c.getId()));
		return c;
	}

	@Transactional(readOnly = true)
	public Customer get(Long id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Không tìm thấy hộ dùng điện " + id));
	}

	@Transactional(readOnly = true)
	public Customer getByUsername(String username) {
		return customerRepository.findByUserUsername(username)
				.orElseThrow(() -> new NotFoundException("Không tìm thấy hộ dùng điện của tài khoản " + username));
	}

	@Transactional(readOnly = true)
	public Page<Customer> search(String keyword, CustomerStatus status, int page) {
		return customerRepository.findAll(SearchSpecs.customers(keyword, status), pageable(page));
	}

	/** Gợi ý hộ đang hoạt động khi chọn hộ để ghi chỉ số. */
	@Transactional(readOnly = true)
	public List<Customer> suggestActive(String keyword) {
		String kw = "%" + keyword.trim().toLowerCase() + "%";
		return customerRepository.searchActive(kw, PageRequest.of(0, 10));
	}

	/** UC13: khách hàng tự sửa thông tin liên hệ. */
	@Transactional
	public Customer updateProfile(Long customerId, ProfileForm form) {
		Customer c = get(customerId);
		applyContact(c, form.getFullName(), form.getAddress(), form.getPhone(), form.getEmail());
		return c;
	}

	/** UC08: quản trị viên sửa; số công tơ/chỉ số ban đầu chỉ đổi được khi chưa có chỉ số nào. */
	@Transactional
	public Customer adminUpdate(Long customerId, CustomerEditForm form) {
		Customer c = get(customerId);
		String meter = form.getMeterNumber().trim().toUpperCase();
		boolean meterChanged = !meter.equals(c.getMeterNumber());
		boolean initialChanged = form.getInitialReading() != c.getInitialReading();

		if ((meterChanged || initialChanged) && readingRepository.existsByCustomerId(c.getId())) {
			throw new BusinessException("meterNumber",
					"Không thể sửa số công tơ hoặc chỉ số ban đầu khi hộ đã có chỉ số điện");
		}
		if (meterChanged && customerRepository.existsByMeterNumberAndIdNot(meter, c.getId())) {
			throw new BusinessException("meterNumber", "Số công tơ đã được đăng ký");
		}
		applyContact(c, form.getFullName(), form.getAddress(), form.getPhone(), form.getEmail());
		c.setMeterNumber(meter);
		c.setInitialReading(form.getInitialReading());
		return c;
	}

	/** Khóa/mở khóa hộ: hộ khóa không đăng nhập được. */
	@Transactional
	public Customer setLocked(Long customerId, boolean locked) {
		Customer c = get(customerId);
		c.setStatus(locked ? CustomerStatus.LOCKED : CustomerStatus.ACTIVE);
		c.getUser().setEnabled(!locked);
		return c;
	}

	/** UC13: đổi mật khẩu. */
	@Transactional
	public void changePassword(String username, PasswordForm form) {
		AppUser user = userRepository.findByUsername(username)
				.orElseThrow(() -> new NotFoundException("Không tìm thấy tài khoản"));
		if (!passwordEncoder.matches(form.getCurrentPassword(), user.getPasswordHash())) {
			throw new BusinessException("currentPassword", "Mật khẩu hiện tại không đúng");
		}
		if (!form.getNewPassword().equals(form.getConfirmPassword())) {
			throw new BusinessException("confirmPassword", "Mật khẩu xác nhận không khớp");
		}
		user.setPasswordHash(passwordEncoder.encode(form.getNewPassword()));
	}

	private void applyContact(Customer c, String fullName, String address, String phone, String emailRaw) {
		String email = emailRaw.trim().toLowerCase();
		if (customerRepository.existsByEmailAndIdNot(email, c.getId())) {
			throw new BusinessException("email", "Email đã được sử dụng");
		}
		c.setFullName(fullName.trim());
		c.setAddress(address.trim());
		c.setPhone(phone.trim());
		c.setEmail(email);
	}

	static Pageable pageable(int page) {
		return PageRequest.of(Math.max(page, 0), 10, org.springframework.data.domain.Sort.by("id").descending());
	}
}
