package com.phenikaa.electricbilling.dto;

import com.phenikaa.electricbilling.domain.Customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Thông tin liên hệ của hộ: khách hàng tự sửa (UC13) và quản trị viên sửa (UC08). */
@Getter
@Setter
public class ProfileForm {

	@NotBlank(message = "Vui lòng nhập họ tên")
	@Size(min = 2, max = 100, message = "Họ tên từ 2 đến 100 ký tự")
	private String fullName;

	@NotBlank(message = "Vui lòng nhập địa chỉ")
	@Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
	private String address;

	@NotBlank(message = "Vui lòng nhập số điện thoại")
	@Pattern(regexp = "^$|^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
	private String phone;

	@NotBlank(message = "Vui lòng nhập email")
	@Email(message = "Email không đúng định dạng")
	@Size(max = 100, message = "Email tối đa 100 ký tự")
	private String email;

	public static ProfileForm of(Customer c) {
		ProfileForm f = new ProfileForm();
		f.setFullName(c.getFullName());
		f.setAddress(c.getAddress());
		f.setPhone(c.getPhone());
		f.setEmail(c.getEmail());
		return f;
	}
}
