package com.phenikaa.electricbilling.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.phenikaa.electricbilling.domain.Bill;
import com.phenikaa.electricbilling.domain.BillStatus;
import com.phenikaa.electricbilling.domain.Customer;
import com.phenikaa.electricbilling.domain.NotificationLog;
import com.phenikaa.electricbilling.domain.NotificationStatus;
import com.phenikaa.electricbilling.domain.NotificationType;
import com.phenikaa.electricbilling.domain.Payment;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.exception.NotFoundException;
import com.phenikaa.electricbilling.repository.BillRepository;
import com.phenikaa.electricbilling.repository.NotificationLogRepository;
import com.phenikaa.electricbilling.repository.PaymentRepository;
import com.phenikaa.electricbilling.web.Fmt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gửi email thông báo và ghi nhật ký (UC10). Lỗi gửi mail không bao giờ làm hỏng nghiệp vụ chính:
 * kết quả được ghi nhật ký với trạng thái SENT / FAILED / SKIPPED.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationLogRepository logRepository;
	private final BillRepository billRepository;
	private final PaymentRepository paymentRepository;
	private final MailService mailService;
	private final SettingsService settings;
	private final Fmt fmt;
	private final Clock clock;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onBillIssued(BillIssuedEvent event) {
		billRepository.findById(event.billId()).ifPresent(b -> send(b, NotificationType.NEW_BILL));
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void onPaymentReceived(PaymentReceivedEvent event) {
		billRepository.findById(event.billId()).ifPresent(b -> send(b, NotificationType.PAYMENT_CONFIRMATION));
	}

	/** Gửi một email và ghi nhật ký; không ném ngoại lệ khi gửi lỗi. */
	@Transactional
	public NotificationLog send(Bill bill, NotificationType type) {
		Customer customer = bill.getCustomer();
		NotificationLog entry = new NotificationLog();
		entry.setCustomer(customer);
		entry.setBill(bill);
		entry.setType(type);
		entry.setToEmail(customer.getEmail());
		entry.setSubject(subject(bill, type));
		entry.setCreatedAt(LocalDateTime.now(clock));

		if (!settings.mailEnabled()) {
			entry.setStatus(NotificationStatus.SKIPPED);
			entry.setError("Gửi email đang tắt trong cấu hình");
		} else {
			try {
				mailService.send(settings.mailFrom(), customer.getEmail(), entry.getSubject(), body(bill, type));
				entry.setStatus(NotificationStatus.SENT);
			} catch (Exception ex) {
				log.warn("Gửi email {} cho hóa đơn {} thất bại: {}", type, bill.getBillNo(), ex.getMessage());
				entry.setStatus(NotificationStatus.FAILED);
				entry.setError(truncate(ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()));
			}
		}
		return logRepository.save(entry);
	}

	/** Tác vụ hằng ngày: nhắc trước hạn N ngày và thông báo quá hạn (mỗi hóa đơn/loại chỉ một lần). */
	@Transactional
	public int sendScheduledNotices() {
		LocalDate today = LocalDate.now(clock);
		int sent = 0;
		for (Bill b : billRepository.findByStatusAndDueDate(BillStatus.UNPAID, today.plusDays(settings.reminderDays()))) {
			sent += sendOnce(b, NotificationType.REMINDER);
		}
		for (Bill b : billRepository.findByStatusAndDueDate(BillStatus.UNPAID, today.minusDays(1))) {
			sent += sendOnce(b, NotificationType.OVERDUE);
		}
		return sent;
	}

	/** ADMIN: gửi nhắc nợ cho mọi hóa đơn chưa thanh toán chưa từng được nhắc/báo quá hạn thành công. Chạy nền. */
	@Async
	@Transactional
	public void sendPendingNoticesAsync() {
		LocalDate today = LocalDate.now(clock);
		for (Bill b : billRepository.findByStatus(BillStatus.UNPAID)) {
			boolean noticed = logRepository.existsByBillIdAndTypeAndStatus(b.getId(), NotificationType.REMINDER,
					NotificationStatus.SENT)
					|| logRepository.existsByBillIdAndTypeAndStatus(b.getId(), NotificationType.OVERDUE,
							NotificationStatus.SENT);
			if (!noticed) {
				send(b, b.isOverdue(today) ? NotificationType.OVERDUE : NotificationType.REMINDER);
			}
		}
	}

	/** Gửi lại một thông báo (thất bại/bỏ qua) bằng một dòng nhật ký mới. */
	@Transactional
	public NotificationLog resend(Long logId) {
		NotificationLog old = logRepository.findById(logId)
				.orElseThrow(() -> new NotFoundException("Không tìm thấy thông báo " + logId));
		if (old.getStatus() == NotificationStatus.SENT) {
			throw new BusinessException("Thông báo này đã gửi thành công");
		}
		Bill bill = old.getBill();
		if (bill == null) {
			throw new BusinessException("Thông báo không gắn với hóa đơn nào");
		}
		boolean needsUnpaid = old.getType() == NotificationType.REMINDER || old.getType() == NotificationType.OVERDUE;
		if (needsUnpaid && bill.getStatus() == BillStatus.PAID) {
			throw new BusinessException("Hóa đơn đã được thanh toán, không cần nhắc nợ");
		}
		return send(bill, old.getType());
	}

	@Transactional(readOnly = true)
	public Page<NotificationLog> search(NotificationType type, NotificationStatus status, int page) {
		return logRepository.findAll(SearchSpecs.notifications(type, status), CustomerService.pageable(page));
	}

	private int sendOnce(Bill bill, NotificationType type) {
		if (logRepository.existsByBillIdAndTypeAndStatus(bill.getId(), type, NotificationStatus.SENT)) {
			return 0;
		}
		send(bill, type);
		return 1;
	}

	private String subject(Bill bill, NotificationType type) {
		String period = fmt.period(bill.getPeriodYear(), bill.getPeriodMonth());
		String prefix = "[" + settings.companyName() + "] ";
		return switch (type) {
			case NEW_BILL -> prefix + "Hóa đơn tiền điện kỳ " + period;
			case REMINDER -> prefix + "Nhắc thanh toán hóa đơn tiền điện kỳ " + period;
			case OVERDUE -> prefix + "Hóa đơn tiền điện kỳ " + period + " đã quá hạn";
			case PAYMENT_CONFIRMATION -> prefix + "Xác nhận thanh toán hóa đơn kỳ " + period;
		};
	}

	private String body(Bill bill, NotificationType type) {
		Customer c = bill.getCustomer();
		String period = fmt.period(bill.getPeriodYear(), bill.getPeriodMonth());
		StringBuilder sb = new StringBuilder();
		sb.append("Kính gửi ").append(c.getFullName()).append(" (mã hộ ").append(c.getCustomerCode()).append("),\n\n");

		switch (type) {
			case NEW_BILL -> sb.append(settings.companyName()).append(" thông báo hóa đơn tiền điện kỳ ")
					.append(period).append(":\n");
			case REMINDER -> sb.append("Hóa đơn tiền điện kỳ ").append(period)
					.append(" của quý khách sắp đến hạn thanh toán (").append(fmt.date(bill.getDueDate()))
					.append("):\n");
			case OVERDUE -> sb.append("Hóa đơn tiền điện kỳ ").append(period)
					.append(" của quý khách đã quá hạn thanh toán từ ngày ").append(fmt.date(bill.getDueDate()))
					.append(":\n");
			case PAYMENT_CONFIRMATION -> sb.append("Chúng tôi đã nhận được thanh toán cho hóa đơn kỳ ")
					.append(period).append(":\n");
		}

		sb.append("- Số hóa đơn: ").append(bill.getBillNo()).append('\n');
		sb.append("- Điện năng tiêu thụ: ").append(bill.getConsumptionKwh()).append(" kWh\n");
		sb.append("- Tiền điện: ").append(fmt.money(bill.getSubtotal())).append('\n');
		sb.append("- VAT (").append(bill.getVatRate().stripTrailingZeros().toPlainString()).append("%): ")
				.append(fmt.money(bill.getVatAmount())).append('\n');
		sb.append("- Tổng cộng: ").append(fmt.money(bill.getTotalAmount())).append('\n');

		if (type == NotificationType.PAYMENT_CONFIRMATION) {
			paymentRepository.findByBillId(bill.getId()).ifPresent((Payment p) -> {
				sb.append("- Mã giao dịch: ").append(p.getReferenceCode()).append('\n');
				sb.append("- Phương thức: ").append(p.getMethod().getLabel()).append('\n');
				sb.append("- Thời gian: ").append(fmt.dateTime(p.getPaidAt())).append('\n');
			});
			sb.append("\nCảm ơn quý khách đã thanh toán.\n");
		} else {
			sb.append("- Hạn thanh toán: ").append(fmt.date(bill.getDueDate())).append('\n');
			sb.append("\nVui lòng đăng nhập hệ thống để thanh toán đúng hạn.\n");
		}
		sb.append("\nTrân trọng,\n").append(settings.companyName());
		return sb.toString();
	}

	private static String truncate(String s) {
		return s.length() <= 500 ? s : s.substring(0, 497) + "...";
	}
}
