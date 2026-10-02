package com.phenikaa.electricbilling.domain;

public enum CustomerStatus {
	ACTIVE("Đang hoạt động"),
	LOCKED("Đã khóa");

	private final String label;

	CustomerStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
