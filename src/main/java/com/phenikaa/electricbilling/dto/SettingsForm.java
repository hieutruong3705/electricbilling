package com.phenikaa.electricbilling.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Form cấu hình hệ thống (UC12): biểu giá bậc thang + tham số vận hành. */
@Getter
@Setter
public class SettingsForm {

	private List<TierForm> tiers = new ArrayList<>();

	@NotNull(message = "Vui lòng nhập VAT")
	@DecimalMin(value = "0", message = "VAT từ 0 đến 100")
	@DecimalMax(value = "100", message = "VAT từ 0 đến 100")
	@Digits(integer = 3, fraction = 2, message = "VAT tối đa 2 chữ số thập phân")
	private BigDecimal vatRate;

	@NotNull(message = "Vui lòng nhập hạn thanh toán")
	@Min(value = 1, message = "Hạn thanh toán từ 1 đến 60 ngày")
	@Max(value = 60, message = "Hạn thanh toán từ 1 đến 60 ngày")
	private Integer dueDays;

	@NotNull(message = "Vui lòng nhập số ngày nhắc trước hạn")
	@Min(value = 0, message = "Nhắc trước hạn từ 0 đến 30 ngày")
	@Max(value = 30, message = "Nhắc trước hạn từ 0 đến 30 ngày")
	private Integer reminderDays;

	private boolean mailEnabled;

	@NotBlank(message = "Vui lòng nhập địa chỉ gửi")
	@Email(message = "Địa chỉ gửi không đúng định dạng email")
	@Size(max = 100, message = "Tối đa 100 ký tự")
	private String mailFrom;

	@NotBlank(message = "Vui lòng nhập tên đơn vị điện lực")
	@Size(max = 100, message = "Tối đa 100 ký tự")
	private String companyName;
}
