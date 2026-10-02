package com.phenikaa.electricbilling.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Chỉ số điện ghi theo kỳ (tháng/năm). */
@Entity
@Table(name = "meter_reading",
		uniqueConstraints = @UniqueConstraint(name = "uk_reading_customer_period",
				columnNames = { "customer_id", "period_year", "period_month" }))
@Getter
@Setter
@NoArgsConstructor
public class MeterReading {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@Column(name = "period_year", nullable = false)
	private int periodYear;

	@Column(name = "period_month", nullable = false)
	private int periodMonth;

	@Column(name = "previous_reading", nullable = false)
	private int previousReading;

	@Column(name = "current_reading", nullable = false)
	private int currentReading;

	@Column(name = "reading_date", nullable = false)
	private LocalDate readingDate;

	@Column(name = "recorded_by", nullable = false, length = 30)
	private String recordedBy;

	/** Điện năng tiêu thụ (kWh) = chỉ số mới − chỉ số cũ. */
	public int getConsumption() {
		return currentReading - previousReading;
	}

	/** Khóa so sánh kỳ: năm*12 + tháng. */
	public int getPeriodIndex() {
		return periodYear * 12 + periodMonth;
	}
}
