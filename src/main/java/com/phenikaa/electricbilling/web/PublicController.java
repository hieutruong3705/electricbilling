package com.phenikaa.electricbilling.web;

import java.util.regex.Pattern;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.phenikaa.electricbilling.dto.RegisterForm;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.service.BillingCalculator;
import com.phenikaa.electricbilling.service.CustomerService;
import com.phenikaa.electricbilling.service.SettingsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Trang công khai: đăng nhập (UC02), đăng ký (UC01), ước tính tiền điện (UC03). */
@Controller
@RequiredArgsConstructor
public class PublicController {

	private static final Pattern KWH = Pattern.compile("^\\d{1,5}$");

	private final CustomerService customerService;
	private final BillingCalculator calculator;
	private final SettingsService settings;

	@GetMapping("/")
	public String home(Authentication auth) {
		boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
		return admin ? "redirect:/admin" : "redirect:/customer/bills";
	}

	@GetMapping("/login")
	public String login() {
		return "login";
	}

	@GetMapping("/register")
	public String registerForm(Model model) {
		model.addAttribute("form", new RegisterForm());
		return "register";
	}

	@PostMapping("/register")
	public String register(@Valid @ModelAttribute("form") RegisterForm form, BindingResult result,
			RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				customerService.register(form);
				redirect.addFlashAttribute("success", "Đăng ký thành công. Vui lòng đăng nhập.");
				return "redirect:/login";
			} catch (BusinessException ex) {
				WebUtils.reject(result, ex);
			}
		}
		form.setPassword(null);
		form.setConfirmPassword(null);
		return "register";
	}

	@GetMapping("/estimate")
	public String estimate(@RequestParam(required = false) String kwh, Model model) {
		if (kwh != null) {
			model.addAttribute("kwh", kwh);
			String trimmed = kwh.trim();
			if (!KWH.matcher(trimmed).matches()) {
				model.addAttribute("estimateError", "Vui lòng nhập số kWh là số nguyên từ 0 đến 99.999");
			} else {
				model.addAttribute("result",
						calculator.calculate(Integer.parseInt(trimmed), settings.tiers(), settings.vatRate()));
			}
		}
		return "estimate";
	}
}
