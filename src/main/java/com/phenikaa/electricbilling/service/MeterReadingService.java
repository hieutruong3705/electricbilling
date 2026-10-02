package com.phenikaa.electricbilling.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phenikaa.electricbilling.domain.Bill;
import com.phenikaa.electricbilling.domain.Customer;
import com.phenikaa.electricbilling.domain.MeterReading;
import com.phenikaa.electricbilling.dto.ReadingForm;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.exception.NotFoundException;
import com.phenikaa.electricbilling.repository.BillRepository;
import com.phenikaa.electricbilling.repository.CustomerRepository;
import com.phenikaa.electricbilling.repository.MeterReadingRepository;

import lombok.RequiredArgsConstructor;

/** Ghi chỉ số điện hằng kỳ và sửa chỉ số kỳ gần nhất (UC04). */
@Service
@RequiredArgsConstructor
public class MeterReadingService {

	private final CustomerRepository customerRepository;
	private final MeterReadingRepository readingRepository;
	private final BillRepository billRepository;
	private final BillService billService;
	private final Clock clock;

	/** Thông tin gợi ý cho form ghi chỉ số của một hộ. */
	public record ReadingContext(Customer customer, int previousReading, int suggestedYear, int suggestedMonth,
			LocalDate suggestedDate) {
	}

	@Transactional(readOnly = true)
	public ReadingContext prepare(Long customerId) {
		Customer customer = customerRepository.findById(customerId)
				.orElseThrow(() -> new NotFoundException("Không tìm thấy hộ dùng điện " + customerId));
		LocalDate today = LocalDate.now(clock);
		Optional<MeterReading> last = readingRepository
				.findFirstByCustomerIdOrderByPeriodYearDescPeriodMonthDesc(customerId);

		int previous = last.map(MeterReading::getCurrentReading).orElse(customer.getInitialReading());
		YearMonth next = last.map(r -> YearMonth.of(r.getPeriodYear(), r.getPeriodMonth()).plusMonths(1))
				.orElse(YearMonth.from(today));
		LocalDate date = today.isBefore(next.atEndOfMonth()) ? today : next.atEndOfMonth();
		return new ReadingContext(customer, previous, next.getYear(), next.getMonthValue(), date);
	}

	/** Ghi chỉ số của một kỳ và lập hóa đơn (cùng một giao dịch). */
	@Transactional
	public Bill record(ReadingForm form, String recordedBy) {
		Customer customer = customerRepository.findById(form.getCustomerId())
				.orElseThrow(() -> new NotFoundException("Không tìm thấy hộ dùng điện " + form.getCustomerId()));
		if (!customer.isActive()) {
			throw new BusinessException("Hộ dùng điện đã bị khóa, không thể ghi chỉ số");
		}

		int year = form.getPeriodYear();
		int month = form.getPeriodMonth();
		YearMonth period = YearMonth.of(year, month);
		LocalDate today = LocalDate.now(clock);

		Optional<MeterReading> last = readingRepository
				.findFirstByCustomerIdOrderByPeriodYearDescPeriodMonthDesc(customer.getId());
		int previous = last.map(MeterReading::getCurrentReading).orElse(customer.getInitialReading());

		if (readingRepository.existsByCustomerIdAndPeriodYearAndPeriodMonth(customer.getId(), year, month)) {
			throw new BusinessException("periodMonth", "Kỳ " + label(period) + " đã có chỉ số");
		}
		if (last.isPresent() && year * 12 + month <= last.get().getPeriodIndex()) {
			throw new BusinessException("periodMonth", "Kỳ phải sau kỳ gần nhất đã ghi ("
					+ label(YearMonth.of(last.get().getPeriodYear(), last.get().getPeriodMonth())) + ")");
		}
		if (form.getReadingDate().isAfter(today)) {
			throw new BusinessException("readingDate", "Ngày ghi chỉ số không được ở tương lai");
		}
		if (!YearMonth.from(form.getReadingDate()).equals(period)) {
			throw new BusinessException("readingDate", "Ngày ghi chỉ số phải thuộc tháng của kỳ " + label(period));
		}
		if (form.getCurrentReading() < previous) {
			throw new BusinessException("currentReading",
					"Chỉ số mới phải lớn hơn hoặc bằng chỉ số cũ (" + previous + ")");
		}

		MeterReading reading = new MeterReading();
		reading.setCustomer(customer);
		reading.setPeriodYear(year);
		reading.setPeriodMonth(month);
		reading.setPreviousReading(previous);
		reading.setCurrentReading(form.getCurrentReading());
		reading.setReadingDate(form.getReadingDate());
		reading.setRecordedBy(recordedBy);
		try {
			reading = readingRepository.saveAndFlush(reading);
		} catch (DataIntegrityViolationException ex) {
			throw new BusinessException("periodMonth", "Kỳ " + label(period) + " đã có chỉ số");
		}
		return billService.issue(reading);
	}

	/** Sửa chỉ số của kỳ gần nhất khi hóa đơn chưa thanh toán (hoặc hóa đơn 0 đồng) và tính lại hóa đơn. */
	@Transactional
	public Bill updateLatest(Long billId, int newCurrentReading, String actor) {
		Bill bill = billRepository.findById(billId)
				.orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn " + billId));
		MeterReading reading = bill.getReading();

		MeterReading latest = readingRepository
				.findFirstByCustomerIdOrderByPeriodYearDescPeriodMonthDesc(bill.getCustomer().getId())
				.orElseThrow();
		if (!latest.getId().equals(reading.getId())) {
			throw new BusinessException("Chỉ được sửa chỉ số của kỳ gần nhất");
		}
		if (bill.isPaid() && bill.getTotalAmount().signum() > 0) {
			throw new BusinessException("Hóa đơn đã thanh toán, không thể sửa chỉ số");
		}
		if (newCurrentReading < reading.getPreviousReading()) {
			throw new BusinessException("currentReading",
					"Chỉ số mới phải lớn hơn hoặc bằng chỉ số cũ (" + reading.getPreviousReading() + ")");
		}
		reading.setCurrentReading(newCurrentReading);
		reading.setRecordedBy(actor);
		return billService.recalculate(bill);
	}

	/** Hóa đơn có thể sửa chỉ số hay không (dùng để ẩn/hiện nút trên giao diện). */
	@Transactional(readOnly = true)
	public boolean canEdit(Bill bill) {
		if (bill.isPaid() && bill.getTotalAmount().signum() > 0) {
			return false;
		}
		return readingRepository.findFirstByCustomerIdOrderByPeriodYearDescPeriodMonthDesc(bill.getCustomer().getId())
				.map(r -> r.getId().equals(bill.getReading().getId()))
				.orElse(false);
	}

	@Transactional(readOnly = true)
	public Page<MeterReading> search(String keyword, YearMonth period, int page) {
		return readingRepository.findAll(SearchSpecs.readings(keyword, period), CustomerService.pageable(page));
	}

	private static String label(YearMonth ym) {
		return String.format("%02d/%d", ym.getMonthValue(), ym.getYear());
	}
}
