package com.phenikaa.electricbilling.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.phenikaa.electricbilling.domain.SystemConfig;
import com.phenikaa.electricbilling.domain.TariffTier;
import com.phenikaa.electricbilling.dto.SettingsForm;
import com.phenikaa.electricbilling.dto.TierForm;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.repository.SystemConfigRepository;
import com.phenikaa.electricbilling.repository.TariffTierRepository;

import lombok.RequiredArgsConstructor;

/** Cấu hình hệ thống: biểu giá bậc thang và các tham số vận hành (UC12). */
@Service
@RequiredArgsConstructor
public class SettingsService {

	public static final String VAT_RATE = "vat_rate";
	public static final String DUE_DAYS = "due_days";
	public static final String REMINDER_DAYS = "reminder_days_before_due";
	public static final String MAIL_ENABLED = "mail_enabled";
	public static final String MAIL_FROM = "mail_from";
	public static final String COMPANY_NAME = "company_name";

	private static final int MAX_TIERS = 20;
	private static final BigDecimal MAX_UNIT_PRICE = new BigDecimal("10000000");

	private final SystemConfigRepository configRepository;
	private final TariffTierRepository tierRepository;

	@Transactional(readOnly = true)
	public BigDecimal vatRate() {
		return new BigDecimal(get(VAT_RATE, "8"));
	}

	@Transactional(readOnly = true)
	public int dueDays() {
		return Integer.parseInt(get(DUE_DAYS, "15"));
	}

	@Transactional(readOnly = true)
	public int reminderDays() {
		return Integer.parseInt(get(REMINDER_DAYS, "3"));
	}

	@Transactional(readOnly = true)
	public boolean mailEnabled() {
		return Boolean.parseBoolean(get(MAIL_ENABLED, "true"));
	}

	@Transactional(readOnly = true)
	public String mailFrom() {
		return get(MAIL_FROM, "no-reply@electricbilling.local");
	}

	@Transactional(readOnly = true)
	public String companyName() {
		return get(COMPANY_NAME, "Điện lực Phenikaa");
	}

	@Transactional(readOnly = true)
	public List<TariffTier> tiers() {
		return tierRepository.findAllByOrderByTierNoAsc();
	}

	@Transactional(readOnly = true)
	public SettingsForm currentForm() {
		SettingsForm form = new SettingsForm();
		for (TariffTier t : tiers()) {
			BigDecimal price = t.getUnitPrice();
			if (price.remainder(BigDecimal.ONE).signum() == 0) {
				price = price.setScale(0); // 1984.00 -> 1984
			}
			form.getTiers().add(new TierForm(t.getFromKwh(), t.getToKwh(), price));
		}
		form.setVatRate(vatRate());
		form.setDueDays(dueDays());
		form.setReminderDays(reminderDays());
		form.setMailEnabled(mailEnabled());
		form.setMailFrom(mailFrom());
		form.setCompanyName(companyName());
		return form;
	}

	/** Lưu toàn bộ cấu hình; biểu giá sai thì không lưu gì cả. */
	@Transactional
	public void save(SettingsForm form) {
		List<TariffTier> newTiers = buildTiers(form.getTiers());

		tierRepository.deleteAllInBatch();
		tierRepository.saveAll(newTiers);

		put(VAT_RATE, form.getVatRate().stripTrailingZeros().toPlainString());
		put(DUE_DAYS, String.valueOf(form.getDueDays()));
		put(REMINDER_DAYS, String.valueOf(form.getReminderDays()));
		put(MAIL_ENABLED, String.valueOf(form.isMailEnabled()));
		put(MAIL_FROM, form.getMailFrom().trim());
		put(COMPANY_NAME, form.getCompanyName().trim());
	}

	/** Khởi tạo dữ liệu mặc định cho các mục còn thiếu. */
	@Transactional
	public void seedDefaults() {
		if (tierRepository.count() == 0) {
			tierRepository.saveAll(List.of(
					new TariffTier(1, 0, 50, new BigDecimal("1984")),
					new TariffTier(2, 50, 100, new BigDecimal("2050")),
					new TariffTier(3, 100, 200, new BigDecimal("2380")),
					new TariffTier(4, 200, 300, new BigDecimal("2998")),
					new TariffTier(5, 300, 400, new BigDecimal("3350")),
					new TariffTier(6, 400, null, new BigDecimal("3460"))));
		}
		putIfAbsent(VAT_RATE, "8");
		putIfAbsent(DUE_DAYS, "15");
		putIfAbsent(REMINDER_DAYS, "3");
		putIfAbsent(MAIL_ENABLED, "true");
		putIfAbsent(MAIL_FROM, "no-reply@electricbilling.local");
		putIfAbsent(COMPANY_NAME, "Điện lực Phenikaa");
	}

	/** Kiểm tra và dựng danh sách bậc: bắt đầu từ 0, liền kề, đơn giá dương, chỉ bậc cuối không giới hạn. */
	List<TariffTier> buildTiers(List<TierForm> forms) {
		List<TierForm> rows = forms == null ? List.of() : forms.stream().filter(t -> !t.isBlank()).toList();
		if (rows.isEmpty()) {
			throw new BusinessException("Cần có ít nhất một bậc giá");
		}
		if (rows.size() > MAX_TIERS) {
			throw new BusinessException("Tối đa " + MAX_TIERS + " bậc giá");
		}

		List<TariffTier> result = new ArrayList<>();
		for (int i = 0; i < rows.size(); i++) {
			TierForm row = rows.get(i);
			int no = i + 1;
			boolean last = i == rows.size() - 1;

			if (row.getFromKwh() == null) {
				throw new BusinessException("Bậc " + no + ": chưa nhập \"Từ kWh\"");
			}
			if (row.getUnitPrice() == null || row.getUnitPrice().signum() <= 0) {
				throw new BusinessException("Bậc " + no + ": đơn giá phải lớn hơn 0");
			}
			if (row.getUnitPrice().compareTo(MAX_UNIT_PRICE) > 0) {
				throw new BusinessException("Bậc " + no + ": đơn giá quá lớn");
			}
			if (i == 0 && row.getFromKwh() != 0) {
				throw new BusinessException("Bậc 1 phải bắt đầu từ 0 kWh");
			}
			if (i > 0 && !row.getFromKwh().equals(rows.get(i - 1).getToKwh())) {
				throw new BusinessException("Bậc " + no + ": \"Từ kWh\" phải bằng \"Đến kWh\" của bậc " + (no - 1));
			}
			if (last) {
				if (row.getToKwh() != null) {
					throw new BusinessException("Bậc cuối phải để trống \"Đến kWh\" (không giới hạn)");
				}
			} else {
				if (row.getToKwh() == null) {
					throw new BusinessException("Bậc " + no + ": chỉ bậc cuối được để trống \"Đến kWh\"");
				}
				if (row.getToKwh() <= row.getFromKwh()) {
					throw new BusinessException("Bậc " + no + ": \"Đến kWh\" phải lớn hơn \"Từ kWh\"");
				}
			}
			result.add(new TariffTier(no, row.getFromKwh(), row.getToKwh(),
					row.getUnitPrice().setScale(2, RoundingMode.HALF_UP)));
		}
		return result;
	}

	private String get(String key, String defaultValue) {
		return configRepository.findById(key).map(SystemConfig::getValue).orElse(defaultValue);
	}

	private void put(String key, String value) {
		configRepository.save(new SystemConfig(key, value));
	}

	private void putIfAbsent(String key, String value) {
		if (!configRepository.existsById(key)) {
			put(key, value);
		}
	}
}
