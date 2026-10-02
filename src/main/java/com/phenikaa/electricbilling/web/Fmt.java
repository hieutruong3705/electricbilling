package com.phenikaa.electricbilling.web;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

/** Định dạng hiển thị (tiền, ngày, kỳ) dùng chung cho giao diện, email và CSV. Gọi trong template: {@code @fmt}. */
@Component("fmt")
public class Fmt {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

	public String number(Number n) {
		if (n == null) {
			return "";
		}
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setGroupingSeparator('.');
		symbols.setDecimalSeparator(',');
		return new DecimalFormat("#,##0.##", symbols).format(n);
	}

	public String money(BigDecimal amount) {
		return amount == null ? "" : number(amount) + " đ";
	}

	public String date(LocalDate d) {
		return d == null ? "" : d.format(DATE);
	}

	public String dateTime(LocalDateTime d) {
		return d == null ? "" : d.format(DATE_TIME);
	}

	public String period(int year, int month) {
		return String.format("%02d/%d", month, year);
	}

	/** Giá trị cho {@code <input type="month">}: yyyy-MM. */
	public String monthValue(int year, int month) {
		return String.format("%d-%02d", year, month);
	}
}
