package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerCredentialUseCase;
import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.PhoneVerificationFailedException;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.User;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCustomerCredentialService implements CustomerCredentialUseCase {

    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;

    public DefaultCustomerCredentialService(
            UserReaderPort userReader,
            UserStorePort userStore,
            PhoneOwnershipVerificationUseCase phoneOwnershipVerification,
            ApplicationEventPublisher eventPublisher,
            PasswordEncoder passwordEncoder) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.phoneOwnershipVerification = phoneOwnershipVerification;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void changePassword(ChangePasswordCommand command) {
        User user = userReader.findByIdForUpdate(command.userId())
                .orElseThrow(NotFoundException.supplier("회원"));
        if (!user.hasLocalPassword()) {
            throw new HappyGalleryException(ErrorCode.LOCAL_PASSWORD_NOT_SET);
        }
        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS);
        }
        updatePassword(user, command.newPassword());
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyPassword(Long userId, String rawPassword) {
        User user = userReader.findById(userId)
                .filter(User::hasLocalPassword)
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    @Override
    @Transactional
    public Long resetPassword(ResetPasswordCommand command) {
        String phone = KoreanPhoneNumber.required(command.phone());
        User user = userReader.findByEmailForUpdate(command.email())
                .filter(candidate -> candidate.isPhoneVerified())
                .filter(candidate -> phone.equals(candidate.getPhone()))
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.PASSWORD_RESET_FAILED));
        try {
            phoneOwnershipVerification.verify(
                    phone,
                    command.verificationCode(),
                    PhoneVerificationPurpose.PASSWORD_RESET);
        } catch (PhoneVerificationFailedException exception) {
            throw new HappyGalleryException(ErrorCode.PASSWORD_RESET_FAILED);
        }
        updatePassword(user, command.newPassword());
        return user.getId();
    }

    private void updatePassword(User user, String newPassword) {
        if (user.hasLocalPassword() && passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new HappyGalleryException(ErrorCode.PASSWORD_UNCHANGED);
        }
        long invalidatedCredentialVersion = user.getCredentialVersion();
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
        userStore.save(user);
        eventPublisher.publishEvent(new CustomerCredentialsChangedEvent(
                user.getId(), invalidatedCredentialVersion));
    }
}
