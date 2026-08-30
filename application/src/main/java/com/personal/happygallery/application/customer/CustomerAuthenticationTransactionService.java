package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort.LoginSnapshot;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    CustomerAuthenticationTransactionService(
            UserReaderPort userReader,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        this.userReader = userReader;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
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
