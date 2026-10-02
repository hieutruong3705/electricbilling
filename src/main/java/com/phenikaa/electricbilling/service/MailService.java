package com.phenikaa.electricbilling.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/** Gửi email qua SMTP; ném ngoại lệ nếu không gửi được (người gọi ghi nhật ký). */
@Service
@RequiredArgsConstructor
public class MailService {

	private final ObjectProvider<JavaMailSender> senderProvider;

	public void send(String from, String to, String subject, String body) {
		JavaMailSender sender = senderProvider.getIfAvailable();
		if (sender == null) {
			throw new IllegalStateException("Chưa cấu hình máy chủ SMTP");
		}
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(from);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		sender.send(message);
	}
}
