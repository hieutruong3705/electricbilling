package com.phenikaa.electricbilling.exception;

/** Lỗi vi phạm quy tắc nghiệp vụ; {@code field} (nếu có) là trường form liên quan. */
public class BusinessException extends RuntimeException {

	private final String field;

	public BusinessException(String message) {
		this(null, message);
	}

	public BusinessException(String field, String message) {
		super(message);
		this.field = field;
	}

	public String getField() {
		return field;
	}
}
