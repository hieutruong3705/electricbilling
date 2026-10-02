package com.phenikaa.electricbilling.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Ảnh chụp một dòng bậc thang tại thời điểm lập hóa đơn. */
@Entity
@Table(name = "bill_line")
@Getter
@Setter
@NoArgsConstructor
public class BillLine {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "bill_id", nullable = false)
	private Bill bill;

	@Column(name = "tier_no", nullable = false)
	private int tierNo;

	@Column(name = "from_kwh", nullable = false)
	private int fromKwh;

	/** null = không giới hạn. */
	@Column(name = "to_kwh")
	private Integer toKwh;

	@Column(nullable = false)
	private int kwh;

	@Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
	private BigDecimal unitPrice;

	@Column(nullable = false, precision = 15, scale = 0)
	private BigDecimal amount;
}
