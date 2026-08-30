package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.User;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultGuestClaimService implements GuestClaimUseCase {

    private final UserReaderPort userReader;
    private final PhoneVerificationAttemptGuard attemptGuard;
    private final GuestClaimTransactionService transactionService;

    public DefaultGuestClaimService(
            UserReaderPort userReader,
            PhoneVerificationAttemptGuard attemptGuard,
            GuestClaimTransactionService transactionService
    ) {
        this.userReader = userReader;
        this.attemptGuard = attemptGuard;
        this.transactionService = transactionService;
    }

    @Override
    public ClaimPreview preview(Long userId) {
        return transactionService.preview(userId);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public ClaimPreview verifyPhoneAndPreview(Long userId, String verificationCode) {
        User user = userReader.findById(userId)
                .orElseThrow(NotFoundException.supplier("회원"));
        String normalizedPhone = KoreanPhoneNumber.required(user.getPhone());
        attemptGuard.check(normalizedPhone);
        return transactionService.verifyPhoneAndPreview(
                userId, normalizedPhone, verificationCode);
    }

    @Override
    public ClaimResult claim(Long userId,
                             List<Long> orderIds,
                             List<Long> bookingIds) {
        return transactionService.claim(userId, orderIds, bookingIds);
    }
}
