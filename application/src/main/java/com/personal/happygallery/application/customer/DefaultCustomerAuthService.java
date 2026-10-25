package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationAttemptGuard;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort.LoginSnapshot;
import com.personal.happygallery.application.policy.PolicyConsentService;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.User;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCustomerAuthService implements CustomerAuthUseCase {

    private static final String DUMMY_PASSWORD_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final UserReaderPort userReader;
    private final PhoneVerificationAttemptGuard attemptGuard;
    private final PasswordEncoder passwordEncoder;
    private final PolicyConsentService policyConsentService;
    private final CustomerAuthenticationTransactionService transactionService;

    public DefaultCustomerAuthService(UserReaderPort userReader,
                                      PhoneVerificationAttemptGuard attemptGuard,
                                      PasswordEncoder passwordEncoder,
                                      PolicyConsentService policyConsentService,
                                      CustomerAuthenticationTransactionService transactionService) {
        this.userReader = userReader;
        this.attemptGuard = attemptGuard;
        this.passwordEncoder = passwordEncoder;
        this.policyConsentService = policyConsentService;
        this.transactionService = transactionService;
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public User signup(SignupCommand command) {
        policyConsentService.requireCurrent(command.policyAcceptance());
        String normalizedPhone = KoreanPhoneNumber.required(command.phone());
        attemptGuard.check(normalizedPhone);
        return transactionService.signup(command, normalizedPhone);
    }

    @Override
    @Transactional(propagation = Propagation.NEVER)
    public User login(LoginCommand command) {
        LoginSnapshot snapshot = userReader.findLoginSnapshotByEmail(command.email()).orElse(null);
        String passwordHash = passwordHashForComparison(snapshot);
        boolean passwordMatches = passwordEncoder.matches(command.rawPassword(), passwordHash);
        if (!isLoginCandidate(snapshot) || !passwordMatches) {
            throw invalidCredentials();
        }
        return transactionService.authenticate(snapshot, command.rawPassword());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findUser(Long userId) {
        return userReader.findById(userId);
    }

    private static String passwordHashForComparison(LoginSnapshot snapshot) {
        return isLoginCandidate(snapshot) ? snapshot.passwordHash() : DUMMY_PASSWORD_HASH;
    }

    private static boolean isLoginCandidate(LoginSnapshot snapshot) {
        return snapshot != null && snapshot.active() && snapshot.hasLocalPassword();
    }

    private static HappyGalleryException invalidCredentials() {
        return new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS);
    }
}
