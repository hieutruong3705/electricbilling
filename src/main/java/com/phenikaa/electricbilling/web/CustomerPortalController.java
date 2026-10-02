package com.phenikaa.electricbilling.web;

import java.security.Principal;

import org.springframework.data.domain.Page;
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
import com.phenikaa.electricbilling.domain.Customer;
import com.phenikaa.electricbilling.domain.Payment;
import com.phenikaa.electricbilling.domain.PaymentMethod;
import com.phenikaa.electricbilling.dto.PasswordForm;
import com.phenikaa.electricbilling.dto.ProfileForm;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.service.BillService;
import com.phenikaa.electricbilling.service.CustomerService;
import com.phenikaa.electricbilling.service.PaymentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Cổng khách hàng: xem/đóng hóa đơn (UC06, UC07) và hồ sơ cá nhân (UC13). Chỉ truy cập dữ liệu của chính mình. */
@Controller
@RequestMapping("/customer")
@RequiredArgsConstructor
public class CustomerPortalController {

	private final CustomerService customerService;
	private final BillService billService;
	private final PaymentService paymentService;

	@GetMapping("/bills")
	public String bills(Principal principal, @RequestParam(required = false) String state,
			@RequestParam(defaultValue = "0") int page, Model model) {
		Customer me = customerService.getByUsername(principal.getName());
		Page<Bill> bills = billService.search(me.getId(), null, null, state, page);
		model.addAttribute("customer", me);
		model.addAttribute("bills", bills);
		model.addAttribute("state", state == null ? "" : state);
		return "customer/bills";
	}

	@GetMapping("/bills/{id}")
	public String billDetail(@PathVariable Long id, Principal principal, Model model) {
		Customer me = customerService.getByUsername(principal.getName());
		Bill bill = billService.getForCustomer(id, me.getId());
		model.addAttribute("bill", bill);
		model.addAttribute("payment", paymentService.findByBill(bill.getId()).orElse(null));
		model.addAttribute("admin", false);
		return "customer/bill-detail";
	}

	@PostMapping("/bills/{id}/pay")
	public String pay(@PathVariable Long id, @RequestParam PaymentMethod method, Principal principal,
			RedirectAttributes redirect) {
		Customer me = customerService.getByUsername(principal.getName());
		try {
			Payment p = paymentService.payOnline(id, me.getId(), method);
			redirect.addFlashAttribute("success", "Thanh toán thành công. Mã giao dịch: " + p.getReferenceCode());
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/customer/bills/" + id;
	}

	@GetMapping("/profile")
	public String profile(Principal principal, Model model) {
		Customer me = customerService.getByUsername(principal.getName());
		model.addAttribute("customer", me);
		model.addAttribute("form", ProfileForm.of(me));
		return "customer/profile";
	}

	@PostMapping("/profile")
	public String updateProfile(@Valid @ModelAttribute("form") ProfileForm form, BindingResult result,
			Principal principal, Model model, RedirectAttributes redirect) {
		Customer me = customerService.getByUsername(principal.getName());
		if (!result.hasErrors()) {
			try {
				customerService.updateProfile(me.getId(), form);
				redirect.addFlashAttribute("success", "Đã cập nhật hồ sơ");
				return "redirect:/customer/profile";
			} catch (BusinessException ex) {
				WebUtils.reject(result, ex);
			}
		}
		model.addAttribute("customer", me);
		return "customer/profile";
	}

	@GetMapping("/password")
	public String passwordForm(Model model) {
		model.addAttribute("form", new PasswordForm());
		return "customer/password";
	}

	@PostMapping("/password")
	public String changePassword(@Valid @ModelAttribute("form") PasswordForm form, BindingResult result,
			Principal principal, RedirectAttributes redirect) {
		if (!result.hasErrors()) {
			try {
				customerService.changePassword(principal.getName(), form);
				redirect.addFlashAttribute("success", "Đã đổi mật khẩu");
				return "redirect:/customer/password";
			} catch (BusinessException ex) {
				WebUtils.reject(result, ex);
			}
		}
		form.setCurrentPassword(null);
		form.setNewPassword(null);
		form.setConfirmPassword(null);
		return "customer/password";
	}
}
