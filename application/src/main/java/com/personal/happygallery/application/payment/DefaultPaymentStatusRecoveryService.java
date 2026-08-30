package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import org.springframework.stereotype.Service;

@Service
public class DefaultPaymentStatusRecoveryService implements PaymentStatusRecoveryUseCase {

    private final PaymentStatusRecoveryTransactionService transactionService;

    public DefaultPaymentStatusRecoveryService(PaymentStatusRecoveryTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @Override
    public RecoveryResult recover(String phone, String verificationCode) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        transactionService.verifyPhoneOwnership(normalizedPhone, verificationCode);
        return transactionService.recover(normalizedPhone);
    }
}
