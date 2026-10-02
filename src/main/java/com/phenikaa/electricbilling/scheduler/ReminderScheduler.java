package com.phenikaa.electricbilling.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.phenikaa.electricbilling.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Chạy hằng ngày (mặc định 08:00) để gửi email nhắc nợ và thông báo quá hạn. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

	private final NotificationService notificationService;

	@Scheduled(cron = "${app.reminder.cron}")
	public void run() {
		int count = notificationService.sendScheduledNotices();
		log.info("Đã xử lý {} thông báo nhắc nợ/quá hạn", count);
	}
}
