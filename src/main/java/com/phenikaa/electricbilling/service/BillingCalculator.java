package com.phenikaa.electricbilling.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.phenikaa.electricbilling.domain.TariffTier;

/**
 * Tính tiền điện bậc thang + VAT (quy tắc BR-06…BR-09). Lớp thuần logic, không truy cập CSDL.
 */
@Component
public class BillingCalculator {

	public BillCalculation calculate(int consumptionKwh, List<TariffTier> tiers, BigDecimal vatRatePercent) {
		if (consumptionKwh < 0) {
			throw new IllegalArgumentException("Điện năng tiêu thụ không được âm");
		}
		if (tiers == null || tiers.isEmpty()) {
			throw new IllegalStateException("Chưa cấu hình biểu giá bậc thang");
		}
		if (vatRatePercent == null || vatRatePercent.signum() < 0) {
			throw new IllegalArgumentException("VAT không hợp lệ");
		}

		List<TariffTier> sorted = tiers.stream().sorted(Comparator.comparingInt(TariffTier::getTierNo)).toList();
		List<BillCalculation.Line> lines = new ArrayList<>();
		BigDecimal subtotal = BigDecimal.ZERO;

		for (TariffTier tier : sorted) {
			int upper = tier.getToKwh() == null ? Integer.MAX_VALUE : tier.getToKwh();
			int kwhInTier = Math.max(0, Math.min(consumptionKwh, upper) - tier.getFromKwh());
			if (kwhInTier == 0) {
				continue;
			}
			BigDecimal amount = tier.getUnitPrice()
					.multiply(BigDecimal.valueOf(kwhInTier))
					.setScale(0, RoundingMode.HALF_UP);
			lines.add(new BillCalculation.Line(tier.getTierNo(), tier.getFromKwh(), tier.getToKwh(),
					kwhInTier, tier.getUnitPrice(), amount));
			subtotal = subtotal.add(amount);
		}

		BigDecimal vat = subtotal.multiply(vatRatePercent)
				.divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
		return new BillCalculation(List.copyOf(lines), subtotal, vatRatePercent, vat, subtotal.add(vat));
	}
}
