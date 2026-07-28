package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCustomerCredentialService implements CustomerCredentialUseCase {

    private final PhoneVerificationAttemptGuard attemptGuard;
    private final CustomerCredentialTransactionService transactionService;

    public DefaultCustomerCredentialService(
            PhoneVerificationAttemptGuard attemptGuard,
            CustomerCredentialTransactionService transactionService
    ) {
        this.attemptGuard = attemptGuard;
        this.transactionService = transactionService;
    }

    @Override
    public void changePassword(ChangePasswordCommand command) {
        transactionService.changePassword(command);
    }

    @Override
    public void verifyPassword(Long userId, String rawPassword) {
        transactionService.verifyPassword(userId, rawPassword);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public Long resetPassword(ResetPasswordCommand command) {
        String normalizedPhone = KoreanPhoneNumber.required(command.phone());
        attemptGuard.check(normalizedPhone);
        return transactionService.resetPassword(command, normalizedPhone);
    }
}
