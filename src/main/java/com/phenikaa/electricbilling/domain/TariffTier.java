package com.phenikaa.electricbilling.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một bậc của biểu giá điện bậc thang hiện hành. */
@Entity
@Table(name = "tariff_tier")
@Getter
@Setter
@NoArgsConstructor
public class TariffTier {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tier_no", nullable = false, unique = true)
	private int tierNo;

	@Column(name = "from_kwh", nullable = false)
	private int fromKwh;

	/** null = không giới hạn (bậc cuối). */
	@Column(name = "to_kwh")
	private Integer toKwh;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	public TariffTier(int tierNo, int fromKwh, Integer toKwh, BigDecimal unitPrice) {
		this.tierNo = tierNo;
		this.fromKwh = fromKwh;
		this.toKwh = toKwh;
		this.unitPrice = unitPrice;
	}
}
