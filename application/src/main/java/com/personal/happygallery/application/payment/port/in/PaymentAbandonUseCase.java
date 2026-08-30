package com.personal.happygallery.application.payment.port.in;

public interface PaymentAbandonUseCase {

    void abandon(String orderId, AuthContext auth, String statusToken);
}
