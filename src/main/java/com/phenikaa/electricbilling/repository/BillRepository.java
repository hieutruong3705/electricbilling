package com.phenikaa.electricbilling.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import com.phenikaa.electricbilling.domain.Bill;
import com.phenikaa.electricbilling.domain.BillStatus;

import jakarta.persistence.LockModeType;

public interface BillRepository extends JpaRepository<Bill, Long>, JpaSpecificationExecutor<Bill> {

	Optional<Bill> findByIdAndCustomerId(Long id, Long customerId);

	Optional<Bill> findByReadingId(Long readingId);

	/** Khóa dòng hóa đơn khi thanh toán để tránh thanh toán trùng. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from Bill b where b.id = :id")
	Optional<Bill> findByIdForUpdate(Long id);

	List<Bill> findByStatus(BillStatus status);

	List<Bill> findByStatusAndDueDate(BillStatus status, LocalDate dueDate);

	long countByStatusAndDueDateGreaterThanEqual(BillStatus status, LocalDate date);

	long countByStatusAndDueDateLessThan(BillStatus status, LocalDate date);

	@Query("select sum(b.totalAmount) from Bill b where b.status = :status")
	BigDecimal sumTotalByStatus(BillStatus status);

	List<Bill> findByPeriodYearAndPeriodMonthOrderByBillNo(int year, int month);

	/** Gộp theo (năm, tháng, trạng thái): [năm, tháng, trạng thái, số hóa đơn, tổng kWh, tổng tiền]. */
	@Query("""
			select b.periodYear, b.periodMonth, b.status, count(b), sum(b.consumptionKwh), sum(b.totalAmount)
			from Bill b
			where b.periodYear * 100 + b.periodMonth between :fromKey and :toKey
			group by b.periodYear, b.periodMonth, b.status
			order by b.periodYear, b.periodMonth
			""")
	List<Object[]> aggregateByPeriod(int fromKey, int toKey);
}
