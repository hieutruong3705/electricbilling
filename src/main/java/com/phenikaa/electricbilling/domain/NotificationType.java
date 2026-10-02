package com.phenikaa.electricbilling.domain;

public enum NotificationType {
	NEW_BILL("Hóa đơn mới"),
	REMINDER("Nhắc thanh toán"),
	OVERDUE("Quá hạn"),
	PAYMENT_CONFIRMATION("Xác nhận thanh toán");

	private final String label;

	NotificationType(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
