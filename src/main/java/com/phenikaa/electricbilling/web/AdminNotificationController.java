package com.phenikaa.electricbilling.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.phenikaa.electricbilling.domain.NotificationLog;
import com.phenikaa.electricbilling.domain.NotificationStatus;
import com.phenikaa.electricbilling.domain.NotificationType;
import com.phenikaa.electricbilling.exception.BusinessException;
import com.phenikaa.electricbilling.service.NotificationService;

import lombok.RequiredArgsConstructor;

/** ADMIN: nhật ký thông báo email, gửi nhắc nợ hàng loạt, gửi lại (UC10). */
@Controller
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public String list(@RequestParam(required = false) String type, @RequestParam(required = false) String status,
			@RequestParam(defaultValue = "0") int page, Model model) {
		model.addAttribute("logs", notificationService.search(WebUtils.parseEnum(NotificationType.class, type),
				WebUtils.parseEnum(NotificationStatus.class, status), page));
		model.addAttribute("type", type == null ? "" : type);
		model.addAttribute("status", status == null ? "" : status);
		return "admin/notifications";
	}

	@PostMapping("/send-pending")
	public String sendPending(RedirectAttributes redirect) {
		notificationService.sendPendingNoticesAsync();
		redirect.addFlashAttribute("success", "Đã bắt đầu gửi nhắc nợ chạy nền. Xem kết quả trong nhật ký bên dưới.");
		return "redirect:/admin/notifications";
	}

	@PostMapping("/{id}/resend")
	public String resend(@PathVariable Long id, RedirectAttributes redirect) {
		try {
			NotificationLog result = notificationService.resend(id);
			if (result.getStatus() == NotificationStatus.SENT) {
				redirect.addFlashAttribute("success", "Đã gửi lại thông báo");
			} else {
				redirect.addFlashAttribute("error",
						"Gửi lại chưa thành công (" + result.getStatus().getLabel() + "): " + result.getError());
			}
		} catch (BusinessException ex) {
			redirect.addFlashAttribute("error", ex.getMessage());
		}
		return "redirect:/admin/notifications";
	}
}
