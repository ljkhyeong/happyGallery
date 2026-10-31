package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultPaymentStatusRecoveryService implements PaymentStatusRecoveryUseCase {

    private final PhoneVerificationAttemptGuard attemptGuard;
    private final PaymentStatusRecoveryTransactionService transactionService;

    public DefaultPaymentStatusRecoveryService(
            PhoneVerificationAttemptGuard attemptGuard,
            PaymentStatusRecoveryTransactionService transactionService
    ) {
        this.attemptGuard = attemptGuard;
        this.transactionService = transactionService;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public RecoveryResult recover(String phone, String verificationCode) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        attemptGuard.check(normalizedPhone);
        transactionService.verifyPhoneOwnership(normalizedPhone, verificationCode);
        return transactionService.recover(normalizedPhone);
    }
}
