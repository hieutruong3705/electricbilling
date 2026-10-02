package com.phenikaa.electricbilling.domain;

public enum BillStatus {
	UNPAID("Chưa thanh toán"),
	PAID("Đã thanh toán");

	private final String label;

	BillStatus(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}
}
