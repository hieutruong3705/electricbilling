package com.phenikaa.electricbilling.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/** Form ghi chỉ số điện của một kỳ (UC04). */
@Getter
@Setter
public class ReadingForm {

	@NotNull(message = "Chưa chọn hộ dùng điện")
	private Long customerId;

	@NotNull(message = "Vui lòng nhập tháng")
	@Min(value = 1, message = "Tháng từ 1 đến 12")
	@Max(value = 12, message = "Tháng từ 1 đến 12")
	private Integer periodMonth;

	@NotNull(message = "Vui lòng nhập năm")
	@Min(value = 2000, message = "Năm từ 2000 đến 2100")
	@Max(value = 2100, message = "Năm từ 2000 đến 2100")
	private Integer periodYear;

	@NotNull(message = "Vui lòng nhập ngày ghi chỉ số")
	@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
	private LocalDate readingDate;

	@NotNull(message = "Vui lòng nhập chỉ số mới")
	@Min(value = 0, message = "Chỉ số mới phải từ 0")
	@Max(value = 99_999_999, message = "Chỉ số mới tối đa 99.999.999")
	private Integer currentReading;
}
