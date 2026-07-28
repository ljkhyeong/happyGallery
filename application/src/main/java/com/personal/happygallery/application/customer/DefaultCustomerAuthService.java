package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort.LoginSnapshot;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.policy.PolicyConsentService;
import com.personal.happygallery.domain.policy.PolicyConsentPurpose;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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
    private final UserStorePort userStore;
    private final PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    private final PasswordEncoder passwordEncoder;
    private final PolicyConsentService policyConsentService;
    private final CustomerAuthenticationTransactionService authenticationService;

    public DefaultCustomerAuthService(UserReaderPort userReader,
                                      UserStorePort userStore,
                                      PhoneOwnershipVerificationUseCase phoneOwnershipVerification,
                                      PasswordEncoder passwordEncoder,
                                      PolicyConsentService policyConsentService,
                                      CustomerAuthenticationTransactionService authenticationService) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.phoneOwnershipVerification = phoneOwnershipVerification;
        this.passwordEncoder = passwordEncoder;
        this.policyConsentService = policyConsentService;
        this.authenticationService = authenticationService;
    }

    @Override
    @Transactional
    public User signup(SignupCommand command) {
        policyConsentService.requireCurrent(command.policyAcceptance());
        phoneOwnershipVerification.verify(
                command.phone(), command.verificationCode(), PhoneVerificationPurpose.SIGNUP);
        if (userReader.existsByEmail(command.email())) {
            throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        if (userReader.existsByPhone(command.phone())) {
            throw new HappyGalleryException(ErrorCode.PHONE_ALREADY_IN_USE);
        }
        User user = new User(
                command.email(),
                passwordEncoder.encode(command.rawPassword()),
                command.name(),
                command.phone());
        user.markPhoneVerified();
        User savedUser = userStore.save(user);
        policyConsentService.recordForUser(
                savedUser.getId(),
                PolicyConsentPurpose.MEMBER_SIGNUP,
                command.policyAcceptance());
        return savedUser;
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
        return authenticationService.authenticate(snapshot, command.rawPassword());
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
