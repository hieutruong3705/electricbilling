package com.phenikaa.electricbilling.web;

import java.security.Principal;

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

import com.phenikaa.electricbilling.domain.Bill;
import com.phenikaa.electricbilling.domain.Payment;
import com.phenikaa.electricbilling.domain.PaymentMethod;
import com.phenikaa.electricbilling.dto.ReadingForm;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.service.BillService;
import com.phenikaa.electricbilling.service.CustomerService;
import com.phenikaa.electricbilling.service.MeterReadingService;
import com.phenikaa.electricbilling.service.MeterReadingService.ReadingContext;
import com.phenikaa.electricbilling.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** ADMIN: ghi chỉ số, hóa đơn, thu tiền (UC04, UC05, UC07, UC09). */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminBillingController {

	private final MeterReadingService readingService;
	private final CustomerService customerService;
	private final BillService billService;
	private final PaymentService paymentService;

	// ---- Chỉ số điện ----

	@GetMapping("/readings")
	public String readings(@RequestParam(required = false) String q, @RequestParam(required = false) String period,
			@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("readings", readingService.search(q, WebUtils.parseMonth(period), page));
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("period", period == null ? "" : period);
		return "admin/readings";
	}

	/** Bước 1: tìm hộ (q); bước 2: có customerId thì hiện form nhập chỉ số. */
	@GetMapping("/readings/new")
	public String newReading(@RequestParam(required = false) Long customerId,
			@RequestParam(required = false) String q, Model model) {
		if (customerId != null) {
			ReadingContext ctx = readingService.prepare(customerId);
			ReadingForm form = new ReadingForm();
			form.setCustomerId(customerId);
			form.setPeriodYear(ctx.suggestedYear());
			form.setPeriodMonth(ctx.suggestedMonth());
			form.setReadingDate(ctx.suggestedDate());
			model.addAttribute("ctx", ctx);
			model.addAttribute("form", form);
		} else if (q != null && !q.isBlank()) {
			model.addAttribute("matches", customerService.suggestActive(q));
		}
		model.addAttribute("q", q == null ? "" : q);
		return "admin/reading-form";
	}

	@PostMapping("/readings")
	public String saveReading(@Valid @ModelAttribute("form") ReadingForm form, BindingResult result,
			Principal principal, Model model, RedirectAttributes redirect) {
		if (form.getCustomerId() == null) {
			return "redirect:/admin/readings/new";
		}
		if (!result.hasErrors()) {
			try {
				Bill bill = readingService.record(form, principal.getName());
				redirect.addFlashAttribute("success", "Đã ghi chỉ số và lập hóa đơn " + bill.getBillNo());
				return "redirect:/admin/bills/" + bill.getId();
			} catch (BusinessException ex) {
				WebUtils.reject(result, ex);
			}
		}
		model.addAttribute("ctx", readingService.prepare(form.getCustomerId()));
		model.addAttribute("q", "");
		return "admin/reading-form";
	}

	// ---- Hóa đơn ----

	@GetMapping("/bills")
	public String bills(@RequestParam(required = false) String q, @RequestParam(required = false) String period,
			@RequestParam(required = false) String state, @RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("bills", billService.search(null, q, WebUtils.parseMonth(period), state, page));
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("period", period == null ? "" : period);
		model.addAttribute("state", state == null ? "" : state);
		return "admin/bills";
	}

	@GetMapping("/bills/{id}")
	public String billDetail(@PathVariable Long id, Model model) {
		Bill bill = billService.get(id);
		model.addAttribute("bill", bill);
		model.addAttribute("payment", paymentService.findByBill(id).orElse(null));
		model.addAttribute("canEditReading", readingService.canEdit(bill));
		model.addAttribute("admin", true);
		return "admin/bill-detail";
	}

	@PostMapping("/bills/{id}/pay")
	public String payAtCounter(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
		try {
			Payment p = paymentService.payAtCounter(id, principal.getName());
			redirect.addFlashAttribute("success", "Đã ghi nhận thu tiền mặt. Mã giao dịch: " + p.getReferenceCode());
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/bills/" + id;
	}

	@PostMapping("/bills/{id}/reading")
	public String editReading(@PathVariable Long id, @RequestParam(required = false) Integer currentReading,
			Principal principal, RedirectAttributes redirect) {
		if (currentReading == null || currentReading < 0 || currentReading > 99_999_999) {
			redirect.addFlashAttribute("error", "Chỉ số mới phải là số nguyên từ 0 đến 99.999.999");
		} else {
			try {
				readingService.updateLatest(id, currentReading, principal.getName());
				redirect.addFlashAttribute("success", "Đã sửa chỉ số và tính lại hóa đơn");
			} catch (BusinessException ex) {
				redirect.addFlashAttribute("error", ex.getMessage());
			}
		}
		return "redirect:/admin/bills/" + id;
	}

	// ---- Thanh toán ----

	@GetMapping("/payments")
	public String payments(@RequestParam(required = false) String q, @RequestParam(required = false) String method,
			@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("payments", paymentService.search(q, WebUtils.parseEnum(PaymentMethod.class, method), page));
		model.addAttribute("q", q == null ? "" : q);
		model.addAttribute("method", method == null ? "" : method);
		return "admin/payments";
	}
}
