package com.phenikaa.electricbilling.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Hộ dùng điện (khách hàng). */
@Entity
@Table(name = "customer")
@Getter
@Setter
@NoArgsConstructor
public class Customer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "customer_code", nullable = false, unique = true, length = 20)
	private String customerCode;

	@OneToOne
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private AppUser user;

	@Column(name = "full_name", nullable = false, length = 100)
	private String fullName;

	@Column(nullable = false, length = 255)
	private String address;

	@Column(nullable = false, length = 15)
	private String phone;

	@Column(nullable = false, unique = true, length = 100)
	private String email;

	@Column(name = "meter_number", nullable = false, unique = true, length = 15)
	private String meterNumber;

	@Column(name = "initial_reading", nullable = false)
	private int initialReading;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private CustomerStatus status = CustomerStatus.ACTIVE;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public boolean isActive() {
		return status == CustomerStatus.ACTIVE;
	}
}
