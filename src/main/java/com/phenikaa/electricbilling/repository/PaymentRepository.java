package com.phenikaa.electricbilling.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.phenikaa.electricbilling.domain.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {

	Optional<Payment> findByBillId(Long billId);

	@Query("select sum(p.amount) from Payment p where p.paidAt >= :from and p.paidAt < :to")
	BigDecimal sumAmountBetween(LocalDateTime from, LocalDateTime to);
}
