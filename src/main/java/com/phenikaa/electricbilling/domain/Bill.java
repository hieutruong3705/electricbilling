package com.phenikaa.electricbilling.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Hóa đơn tiền điện của một kỳ. */
@Entity
@Table(name = "bill")
@Getter
@Setter
@NoArgsConstructor
public class Bill {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "bill_no", nullable = false, unique = true, length = 40)
	private String billNo;

	@ManyToOne(optional = false)
	@JoinColumn(name = "customer_id", nullable = false)
	private Customer customer;

	@OneToOne(optional = false)
	@JoinColumn(name = "reading_id", nullable = false, unique = true)
	private MeterReading reading;

	@Column(name = "period_year", nullable = false)
	private int periodYear;

	@Column(name = "period_month", nullable = false)
	private int periodMonth;

	@Column(name = "consumption_kwh", nullable = false)
	private int consumptionKwh;

	@Column(nullable = false, precision = 15, scale = 0)
	private BigDecimal subtotal;

	@Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal vatRate;

	@Column(name = "vat_amount", nullable = false, precision = 15, scale = 0)
	private BigDecimal vatAmount;

	@Column(name = "total_amount", nullable = false, precision = 15, scale = 0)
	private BigDecimal totalAmount;

	@Column(name = "issue_date", nullable = false)
	private LocalDate issueDate;

	@Column(name = "due_date", nullable = false)
	private LocalDate dueDate;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private BillStatus status = BillStatus.UNPAID;

	@OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
	@OrderBy("tierNo ASC")
	private List<BillLine> lines = new ArrayList<>();

	public void addLine(BillLine line) {
		line.setBill(this);
		lines.add(line);
	}

	public boolean isPaid() {
		return status == BillStatus.PAID;
	}

	/** Quá hạn = chưa thanh toán và ngày hiện tại sau hạn thanh toán. */
	public boolean isOverdue(LocalDate today) {
		return status == BillStatus.UNPAID && today.isAfter(dueDate);
	}

	/** Nhãn trạng thái hiển thị: Chưa thanh toán / Quá hạn / Đã thanh toán. */
	public String statusLabel(LocalDate today) {
		return isOverdue(today) ? "Quá hạn" : status.getLabel();
	}
}
