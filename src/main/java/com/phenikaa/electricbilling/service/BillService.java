package com.phenikaa.electricbilling.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phenikaa.electricbilling.domain.Bill;
import com.phenikaa.electricbilling.domain.BillLine;
import com.phenikaa.electricbilling.domain.BillStatus;
import com.phenikaa.electricbilling.domain.Customer;
import com.phenikaa.electricbilling.domain.MeterReading;
import com.phenikaa.electricbilling.exception.NotFoundException;
import com.phenikaa.electricbilling.repository.BillRepository;

import lombok.RequiredArgsConstructor;

/** Lập hóa đơn, tính lại hóa đơn và tra cứu hóa đơn (UC05, UC06). */
@Service
@RequiredArgsConstructor
public class BillService {

	private final BillRepository billRepository;
	private final BillingCalculator calculator;
	private final SettingsService settings;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	/** UC05: tính tiền theo biểu giá/VAT hiện hành và lập hóa đơn cho kỳ của chỉ số. */
	@Transactional
	public Bill issue(MeterReading reading) {
		Customer customer = reading.getCustomer();
		LocalDate today = LocalDate.now(clock);

		Bill bill = new Bill();
		bill.setBillNo(String.format("HD%d%02d-%s", reading.getPeriodYear(), reading.getPeriodMonth(),
				customer.getCustomerCode()));
		bill.setCustomer(customer);
		bill.setReading(reading);
		bill.setPeriodYear(reading.getPeriodYear());
		bill.setPeriodMonth(reading.getPeriodMonth());
		bill.setIssueDate(today);
		bill.setDueDate(today.plusDays(settings.dueDays()));
		applyCalculation(bill, reading.getConsumption());

		bill = billRepository.save(bill);
		events.publishEvent(new BillIssuedEvent(bill.getId()));
		return bill;
	}

	/** Tính lại hóa đơn sau khi sửa chỉ số (theo biểu giá/VAT hiện hành), giữ nguyên ngày lập và hạn. */
	@Transactional
	public Bill recalculate(Bill bill) {
		applyCalculation(bill, bill.getReading().getConsumption());
		return billRepository.save(bill);
	}

	@Transactional(readOnly = true)
	public Bill get(Long id) {
		return billRepository.findById(id).orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn " + id));
	}

	/** Hóa đơn của một hộ; hóa đơn của hộ khác coi như không tồn tại. */
	@Transactional(readOnly = true)
	public Bill getForCustomer(Long id, Long customerId) {
		return billRepository.findByIdAndCustomerId(id, customerId)
				.orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn " + id));
	}

	@Transactional(readOnly = true)
	public Page<Bill> search(Long customerId, String keyword, YearMonth period, String state, int page) {
		return billRepository.findAll(
				SearchSpecs.bills(customerId, keyword, period, state, LocalDate.now(clock)),
				CustomerService.pageable(page));
	}

	private void applyCalculation(Bill bill, int consumptionKwh) {
		BillCalculation calc = calculator.calculate(consumptionKwh, settings.tiers(), settings.vatRate());

		bill.getLines().clear();
		for (BillCalculation.Line l : calc.lines()) {
			BillLine line = new BillLine();
			line.setTierNo(l.tierNo());
			line.setFromKwh(l.fromKwh());
			line.setToKwh(l.toKwh());
			line.setKwh(l.kwh());
			line.setUnitPrice(l.unitPrice());
			line.setAmount(l.amount());
			bill.addLine(line);
		}
		bill.setConsumptionKwh(consumptionKwh);
		bill.setSubtotal(calc.subtotal());
		bill.setVatRate(calc.vatRate());
		bill.setVatAmount(calc.vatAmount());
		bill.setTotalAmount(calc.total());
		// Hóa đơn 0 đồng không cần thu tiền
		bill.setStatus(calc.total().signum() == 0 ? BillStatus.PAID : BillStatus.UNPAID);
	}
}
