package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase.SignupCommand;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort.LoginSnapshot;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.policy.PolicyConsentService;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.policy.PolicyConsentPurpose;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CustomerAuthenticationTransactionService {

    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final PhoneVerificationConsumptionService phoneVerification;
    private final PolicyConsentService policyConsentService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    CustomerAuthenticationTransactionService(
            UserReaderPort userReader,
            UserStorePort userStore,
            PhoneVerificationConsumptionService phoneVerification,
            PolicyConsentService policyConsentService,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.phoneVerification = phoneVerification;
        this.policyConsentService = policyConsentService;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    /** 시도 제한을 통과한 가입 요청의 인증 코드 소비, 중복 검사, 회원 저장과 동의 기록을 한 트랜잭션으로 처리한다. */
    @Transactional
    public User signup(SignupCommand command, String normalizedPhone) {
        phoneVerification.consume(
                normalizedPhone, command.verificationCode(), PhoneVerificationPurpose.SIGNUP);
        if (userReader.existsByEmail(command.email())) {
            throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userReader.existsByPhone(normalizedPhone)) {
            throw new HappyGalleryException(ErrorCode.PHONE_ALREADY_IN_USE);
        }
        User user = new User(
                command.email(),
                passwordEncoder.encode(command.rawPassword()),
                command.name(),
                normalizedPhone);
        user.markPhoneVerified();
        User savedUser = userStore.save(user);
        policyConsentService.recordForUser(
                savedUser.getId(),
                PolicyConsentPurpose.MEMBER_SIGNUP,
                command.policyAcceptance());
        return savedUser;
    }

    @Transactional
    public User authenticate(LoginSnapshot snapshot, String rawPassword) {
        User user = userReader.findByIdForUpdate(snapshot.userId())
                .orElseThrow(CustomerAuthenticationTransactionService::invalidCredentials);
        if (!passwordStillMatches(snapshot.passwordHash(), rawPassword, user)) {
            throw invalidCredentials();
        }

        if (passwordEncoder.upgradeEncoding(user.getPasswordHash())) {
            user.upgradePasswordHash(passwordEncoder.encode(rawPassword));
        }
        user.updateLastLoginAt(LocalDateTime.now(clock));
        return user;
    }

    private boolean passwordStillMatches(
            String passwordHashBeforeLock,
            String rawPassword,
            User lockedUser
    ) {
        if (!lockedUser.isActive() || !lockedUser.hasLocalPassword()) {
            return false;
        }
        return Objects.equals(passwordHashBeforeLock, lockedUser.getPasswordHash())
                || passwordEncoder.matches(rawPassword, lockedUser.getPasswordHash());
    }

    private static HappyGalleryException invalidCredentials() {
        return new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS);
    }
}
