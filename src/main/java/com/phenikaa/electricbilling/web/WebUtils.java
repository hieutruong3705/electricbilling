package com.phenikaa.electricbilling.web;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.validation.BindingResult;

import com.phenikaa.electricbilling.exception.BusinessException;

/** Tiện ích nhỏ dùng chung cho các controller. */
public final class WebUtils {

	private WebUtils() {
	}

	/** Đưa lỗi nghiệp vụ vào BindingResult: gắn vào trường nếu có, không thì lỗi chung. */
	public static void reject(BindingResult result, BusinessException ex) {
		if (ex.getField() != null) {
			result.rejectValue(ex.getField(), "business", ex.getMessage());
		} else {
			result.reject("business", ex.getMessage());
		}
	}

	/** Đọc "yyyy-MM" (ô nhập tháng); giá trị rỗng/sai trả về null. */
	public static YearMonth parseMonth(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return YearMonth.parse(value.trim());
		} catch (DateTimeParseException ex) {
			return null;
		}
	}

	/** Đọc enum theo tên; rỗng/sai trả về null. */
	public static <E extends Enum<E>> E parseEnum(Class<E> type, String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Enum.valueOf(type, value.trim());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
