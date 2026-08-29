package com.personal.happygallery.application.payment.port.in;

public interface PaymentWebhookUseCase {

    void receive(String transmissionId, String eventType, String orderId);
}
