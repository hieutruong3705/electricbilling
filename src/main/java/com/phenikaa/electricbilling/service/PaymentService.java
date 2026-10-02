package com.phenikaa.electricbilling.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phenikaa.electricbilling.domain.Bill;
import com.phenikaa.electricbilling.domain.BillStatus;
import com.phenikaa.electricbilling.domain.Payment;
import com.phenikaa.electricbilling.domain.PaymentMethod;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.exception.NotFoundException;
import com.phenikaa.electricbilling.repository.BillRepository;
import com.phenikaa.electricbilling.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

/** Đóng tiền hóa đơn: trực tuyến (mô phỏng) hoặc tiền mặt tại quầy (UC07). */
@Service
@RequiredArgsConstructor
public class PaymentService {

	private final BillRepository billRepository;
	private final PaymentRepository paymentRepository;
	private final ApplicationEventPublisher events;
	private final Clock clock;

	/** Khách hàng thanh toán trực tuyến hóa đơn của chính mình (chuyển khoản / ví điện tử). */
	@Transactional
	public Payment payOnline(Long billId, Long customerId, PaymentMethod method) {
		if (method == null || method == PaymentMethod.CASH) {
			throw new BusinessException("Phương thức thanh toán không hợp lệ");
		}
		Bill bill = billRepository.findByIdForUpdate(billId)
				.filter(b -> b.getCustomer().getId().equals(customerId))
				.orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn " + billId));
		return pay(bill, method, bill.getCustomer().getUser().getUsername());
	}

	/** Quản trị viên ghi nhận thu tiền mặt tại quầy. */
	@Transactional
	public Payment payAtCounter(Long billId, String adminUsername) {
		Bill bill = billRepository.findByIdForUpdate(billId)
				.orElseThrow(() -> new NotFoundException("Không tìm thấy hóa đơn " + billId));
		return pay(bill, PaymentMethod.CASH, adminUsername);
	}

	@Transactional(readOnly = true)
	public java.util.Optional<Payment> findByBill(Long billId) {
		return paymentRepository.findByBillId(billId);
	}

	@Transactional(readOnly = true)
	public Page<Payment> search(String keyword, PaymentMethod method, int page) {
		return paymentRepository.findAll(SearchSpecs.payments(keyword, method), CustomerService.pageable(page));
	}

	private Payment pay(Bill bill, PaymentMethod method, String receivedBy) {
		if (bill.getStatus() == BillStatus.PAID) {
			throw new BusinessException("Hóa đơn đã được thanh toán");
		}
		Payment payment = new Payment();
		payment.setBill(bill);
		payment.setAmount(bill.getTotalAmount());
		payment.setMethod(method);
		payment.setReferenceCode("PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
		payment.setPaidAt(LocalDateTime.now(clock));
		payment.setReceivedBy(receivedBy);
		payment = paymentRepository.save(payment);

		bill.setStatus(BillStatus.PAID);
		events.publishEvent(new PaymentReceivedEvent(bill.getId()));
		return payment;
	}
}
