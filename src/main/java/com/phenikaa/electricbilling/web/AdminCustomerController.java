package com.phenikaa.electricbilling.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.phenikaa.electricbilling.domain.Customer;
import com.phenikaa.electricbilling.domain.CustomerStatus;
import com.phenikaa.electricbilling.dto.CustomerEditForm;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.repository.MeterReadingRepository;
import com.phenikaa.electricbilling.service.BillService;
import com.phenikaa.electricbilling.service.CustomerService;
import com.phenikaa.electricbilling.service.ReportService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** ADMIN: bảng điều khiển và quản lý hộ dùng điện (UC08, UC09). */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminCustomerController {

	private final CustomerService customerService;
	private final BillService billService;
	private final ReportService reportService;
	private final MeterReadingRepository readingRepository;

	@GetMapping
	public String dashboard(Model model) {
		model.addAttribute("stats", reportService.dashboard());
		return "admin/dashboard";
	}

	@GetMapping("/customers")
	public String list(@RequestParam(required = false) String q, @RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("customers",
				customerService.search(q, WebUtils.parseEnum(CustomerStatus.class, status), page));
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("status", status == null ? "" : status);
		return "admin/customers";
	}

	@GetMapping("/customers/{id}")
	public String detail(@PathVariable Long id, @RequestParam(defaultValue = "0") int page, Model model) {
		Customer customer = customerService.get(id);
		model.addAttribute("customer", customer);
		model.addAttribute("bills", billService.search(id, null, null, null, page));
		return "admin/customer-detail";
	}

	@GetMapping("/customers/{id}/edit")
	public String editForm(@PathVariable Long id, Model model) {
		Customer customer = customerService.get(id);
		model.addAttribute("customer", customer);
		model.addAttribute("form", CustomerEditForm.of(customer));
		model.addAttribute("hasReadings", readingRepository.existsByCustomerId(id));
		return "admin/customer-edit";
	}

	@PostMapping("/customers/{id}/edit")
	public String edit(@PathVariable Long id, @Valid @ModelAttribute("form") CustomerEditForm form,
			BindingResult result, Model model, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				customerService.adminUpdate(id, form);
				redirect.addFlashAttribute("success", "Đã cập nhật thông tin hộ dùng điện");
				return "redirect:/admin/customers/" + id;
			} catch (BusinessException ex) {
				WebUtils.reject(result, ex);
			}
		}
		model.addAttribute("customer", customerService.get(id));
		model.addAttribute("hasReadings", readingRepository.existsByCustomerId(id));
		return "admin/customer-edit";
	}

	@PostMapping("/customers/{id}/lock")
	public String lock(@PathVariable Long id, RedirectAttributes redirect) {
		customerService.setLocked(id, true);
		redirect.addFlashAttribute("success", "Đã khóa hộ dùng điện");
		return "redirect:/admin/customers/" + id;
	}

	@PostMapping("/customers/{id}/unlock")
	public String unlock(@PathVariable Long id, RedirectAttributes redirect) {
		customerService.setLocked(id, false);
		redirect.addFlashAttribute("success", "Đã mở khóa hộ dùng điện");
		return "redirect:/admin/customers/" + id;
	}
}
