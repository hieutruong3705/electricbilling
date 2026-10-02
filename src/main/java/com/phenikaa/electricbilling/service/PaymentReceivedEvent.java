package com.phenikaa.electricbilling.service;

/** Phát ra khi một hóa đơn được thanh toán (xử lý sau khi giao dịch commit). */
public record PaymentReceivedEvent(Long billId) {
}
