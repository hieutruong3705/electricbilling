package com.phenikaa.electricbilling.service;

import java.math.BigDecimal;
import java.util.List;

/** Kết quả tính tiền điện: các dòng bậc, tiền điện trước thuế, VAT và tổng cộng. */
public record BillCalculation(
		List<Line> lines,
		BigDecimal subtotal,
		BigDecimal vatRate,
		BigDecimal vatAmount,
		BigDecimal total) {

	/** Một bậc có phát sinh điện năng ({@code toKwh == null}: không giới hạn). */
	public record Line(int tierNo, int fromKwh, Integer toKwh, int kwh, BigDecimal unitPrice, BigDecimal amount) {
	}
}
