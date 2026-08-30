package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase;
import com.personal.happygallery.application.customer.port.in.MemberPhoneUpdateUseCase.UpdatePhoneCommand;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultMemberPhoneUpdateService implements MemberPhoneUpdateUseCase {

    private final PhoneVerificationAttemptGuard attemptGuard;
    private final MemberPhoneUpdateTransactionService transactionService;

    public DefaultMemberPhoneUpdateService(
            PhoneVerificationAttemptGuard attemptGuard,
            MemberPhoneUpdateTransactionService transactionService
    ) {
        this.attemptGuard = attemptGuard;
        this.transactionService = transactionService;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public User update(UpdatePhoneCommand command) {
        String normalizedPhone = KoreanPhoneNumber.required(command.phone());
        attemptGuard.check(normalizedPhone);
        return transactionService.update(command, normalizedPhone);
    }
}
