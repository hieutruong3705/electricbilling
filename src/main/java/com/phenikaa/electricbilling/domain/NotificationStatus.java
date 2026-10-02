package com.phenikaa.electricbilling.domain;

public enum NotificationStatus {
	SENT("Đã gửi"),
	FAILED("Thất bại"),
	SKIPPED("Bỏ qua");

	private final String label;

	NotificationStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
