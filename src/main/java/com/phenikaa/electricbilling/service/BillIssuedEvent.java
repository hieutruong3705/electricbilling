package com.phenikaa.electricbilling.service;

/** Phát ra khi hóa đơn mới được lập (xử lý sau khi giao dịch commit). */
public record BillIssuedEvent(Long billId) {
}
