package com.phenikaa.electricbilling.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.phenikaa.electricbilling.dto.SettingsForm;
import com.phenikaa.electricbilling.dto.TierForm;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.service.SettingsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** ADMIN: cấu hình biểu giá bậc thang và tham số vận hành (UC12). */
@Controller
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {

	/** Số dòng trống thêm vào cuối để có thể thêm bậc mới. */
	private static final int EXTRA_ROWS = 2;

	private final SettingsService settingsService;

	@GetMapping
	public String form(Model model) {
		SettingsForm form = settingsService.currentForm();
		addBlankRows(form);
		model.addAttribute("form", form);
		return "admin/settings";
	}

	@PostMapping
	public String save(@Valid @ModelAttribute("form") SettingsForm form, BindingResult result,
			RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				settingsService.save(form);
				redirect.addFlashAttribute("success", "Đã lưu cấu hình");
				return "redirect:/admin/settings";
			} catch (BusinessException ex) {
				WebUtils.reject(result, ex);
			}
		}
		addBlankRows(form);
		return "admin/settings";
	}

	private static void addBlankRows(SettingsForm form) {
		long blanks = form.getTiers().stream().filter(TierForm::isBlank).count();
		for (long i = blanks; i < EXTRA_ROWS; i++) {
			form.getTiers().add(new TierForm());
		}
	}
}
