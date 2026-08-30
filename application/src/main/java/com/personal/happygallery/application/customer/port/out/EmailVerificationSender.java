package com.personal.happygallery.application.customer.port.out;

public interface EmailVerificationSender {

    boolean send(String email, String verificationCode);
}
