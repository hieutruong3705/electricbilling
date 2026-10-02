package com.phenikaa.electricbilling.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phenikaa.electricbilling.domain.Bill;
import com.phenikaa.electricbilling.domain.BillStatus;
import com.phenikaa.electricbilling.domain.CustomerStatus;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.repository.BillRepository;
import com.phenikaa.electricbilling.repository.CustomerRepository;
import com.phenikaa.electricbilling.repository.PaymentRepository;
import com.phenikaa.electricbilling.web.Fmt;

import lombok.RequiredArgsConstructor;

/** Bảng điều khiển và báo cáo (UC09, UC11). */
@Service
@RequiredArgsConstructor
public class ReportService {

	private final BillRepository billRepository;
	private final CustomerRepository customerRepository;
	private final PaymentRepository paymentRepository;
	private final Fmt fmt;
	private final Clock clock;

	public record Dashboard(long activeCustomers, long unpaidBills, long overdueBills, BigDecimal outstanding,
			BigDecimal collectedThisMonth) {
	}

	public record RevenueRow(int year, int month, long billCount, long totalKwh, BigDecimal issued,
			BigDecimal collected, BigDecimal outstanding) {
	}

	public record DebtRow(Bill bill, long overdueDays) {
	}

	@Transactional(readOnly = true)
	public Dashboard dashboard() {
		LocalDate today = LocalDate.now(clock);
		YearMonth month = YearMonth.from(today);
		LocalDateTime from = month.atDay(1).atStartOfDay();
		LocalDateTime to = month.plusMonths(1).atDay(1).atStartOfDay();
		return new Dashboard(
				customerRepository.countByStatus(CustomerStatus.ACTIVE),
				billRepository.countByStatusAndDueDateGreaterThanEqual(BillStatus.UNPAID, today),
				billRepository.countByStatusAndDueDateLessThan(BillStatus.UNPAID, today),
				nz(billRepository.sumTotalByStatus(BillStatus.UNPAID)),
				nz(paymentRepository.sumAmountBetween(from, to)));
	}

	/** Doanh thu theo kỳ từ kỳ {@code from} đến kỳ {@code to} (gồm cả hai đầu). */
	@Transactional(readOnly = true)
	public List<RevenueRow> revenue(YearMonth from, YearMonth to) {
		if (from.isAfter(to)) {
			throw new BusinessException("\"Từ kỳ\" phải trước hoặc bằng \"đến kỳ\"");
		}
		Map<Integer, long[]> counts = new TreeMap<>(); // [số HĐ, kWh]
		Map<Integer, BigDecimal[]> money = new TreeMap<>(); // [phát hành, đã thu, còn nợ]
		for (Object[] r : billRepository.aggregateByPeriod(key(from), key(to))) {
			int year = (Integer) r[0];
			int month = (Integer) r[1];
			BillStatus status = (BillStatus) r[2];
			long count = (Long) r[3];
			long kwh = r[4] == null ? 0 : ((Number) r[4]).longValue();
			BigDecimal total = nz((BigDecimal) r[5]);

			int k = year * 100 + month;
			long[] c = counts.computeIfAbsent(k, x -> new long[2]);
			c[0] += count;
			c[1] += kwh;
			BigDecimal[] m = money.computeIfAbsent(k, x -> new BigDecimal[] { BigDecimal.ZERO, BigDecimal.ZERO,
					BigDecimal.ZERO });
			m[0] = m[0].add(total);
			if (status == BillStatus.PAID) {
				m[1] = m[1].add(total);
			} else {
				m[2] = m[2].add(total);
			}
		}
		List<RevenueRow> rows = new ArrayList<>();
		counts.forEach((k, c) -> {
			BigDecimal[] m = money.get(k);
			rows.add(new RevenueRow(k / 100, k % 100, c[0], c[1], m[0], m[1], m[2]));
		});
		return rows;
	}

	/** Danh sách hóa đơn chưa thanh toán (có thể chỉ lấy quá hạn), hạn sớm nhất trước. */
	@Transactional(readOnly = true)
	public List<DebtRow> debts(boolean overdueOnly) {
		LocalDate today = LocalDate.now(clock);
		return billRepository.findByStatus(BillStatus.UNPAID).stream()
				.filter(b -> !overdueOnly || b.isOverdue(today))
				.sorted(Comparator.comparing(Bill::getDueDate).thenComparing(Bill::getBillNo))
				.map(b -> new DebtRow(b, Math.max(0, ChronoUnit.DAYS.between(b.getDueDate(), today))))
				.toList();
	}

	/** Tiêu thụ và tiền điện của mọi hộ trong một kỳ. */
	@Transactional(readOnly = true)
	public List<Bill> consumption(YearMonth period) {
		return billRepository.findByPeriodYearAndPeriodMonthOrderByBillNo(period.getYear(), period.getMonthValue());
	}

	public byte[] revenueCsv(List<RevenueRow> rows) {
		List<List<String>> data = rows.stream().map(r -> List.of(
				fmt.period(r.year(), r.month()), String.valueOf(r.billCount()), String.valueOf(r.totalKwh()),
				r.issued().toPlainString(), r.collected().toPlainString(), r.outstanding().toPlainString())).toList();
		return CsvWriter.build(List.of("Kỳ", "Số hóa đơn", "Tổng kWh", "Tổng tiền phát hành", "Đã thu", "Còn nợ"), data);
	}

	public byte[] debtsCsv(List<DebtRow> rows) {
		List<List<String>> data = rows.stream().map(r -> List.of(
				r.bill().getCustomer().getCustomerCode(), r.bill().getCustomer().getFullName(),
				r.bill().getCustomer().getPhone(), r.bill().getCustomer().getEmail(), r.bill().getBillNo(),
				fmt.period(r.bill().getPeriodYear(), r.bill().getPeriodMonth()), fmt.date(r.bill().getDueDate()),
				r.bill().getTotalAmount().toPlainString(), String.valueOf(r.overdueDays()))).toList();
		return CsvWriter.build(List.of("Mã hộ", "Họ tên", "Số điện thoại", "Email", "Số hóa đơn", "Kỳ", "Hạn thanh toán",
				"Số tiền", "Số ngày quá hạn"), data);
	}

	public byte[] consumptionCsv(List<Bill> bills) {
		List<List<String>> data = bills.stream().map(b -> List.of(
				b.getCustomer().getCustomerCode(), b.getCustomer().getFullName(), b.getCustomer().getMeterNumber(),
				String.valueOf(b.getReading().getPreviousReading()), String.valueOf(b.getReading().getCurrentReading()),
				String.valueOf(b.getConsumptionKwh()), b.getTotalAmount().toPlainString())).toList();
		return CsvWriter.build(List.of("Mã hộ", "Họ tên", "Số công tơ", "Chỉ số cũ", "Chỉ số mới", "kWh", "Tiền"), data);
	}

	private static int key(YearMonth ym) {
		return ym.getYear() * 100 + ym.getMonthValue();
	}

	private static BigDecimal nz(BigDecimal v) {
		return v == null ? BigDecimal.ZERO : v;
	}
}
