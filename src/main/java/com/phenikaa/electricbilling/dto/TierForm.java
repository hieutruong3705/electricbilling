package com.phenikaa.electricbilling.dto;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Một dòng bậc thang trên form cấu hình. {@code toKwh} để trống = không giới hạn. */
@Getter
@Setter
@NoArgsConstructor
public class TierForm {

	private Integer fromKwh;
	private Integer toKwh;
	private BigDecimal unitPrice;

	public TierForm(Integer fromKwh, Integer toKwh, BigDecimal unitPrice) {
		this.fromKwh = fromKwh;
		this.toKwh = toKwh;
		this.unitPrice = unitPrice;
	}

	/** Dòng trống hoàn toàn được bỏ qua khi lưu (cách xóa một bậc). */
	public boolean isBlank() {
		return fromKwh == null && toKwh == null && unitPrice == null;
	}
}
