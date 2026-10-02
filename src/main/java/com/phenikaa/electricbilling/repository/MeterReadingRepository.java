package com.phenikaa.electricbilling.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.phenikaa.electricbilling.domain.MeterReading;

public interface MeterReadingRepository
		extends JpaRepository<MeterReading, Long>, JpaSpecificationExecutor<MeterReading> {

	/** Chỉ số của kỳ gần nhất. */
	Optional<MeterReading> findFirstByCustomerIdOrderByPeriodYearDescPeriodMonthDesc(Long customerId);

	boolean existsByCustomerIdAndPeriodYearAndPeriodMonth(Long customerId, int year, int month);

	boolean existsByCustomerId(Long customerId);
}
