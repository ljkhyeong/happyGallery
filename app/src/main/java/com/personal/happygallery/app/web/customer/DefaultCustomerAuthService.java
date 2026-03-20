package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.customer.port.out.UserStorePort;
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultCustomerAuthService implements CustomerAuthUseCase {

    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public DefaultCustomerAuthService(UserReaderPort userReader,
                                      UserStorePort userStore,
                                      PasswordEncoder passwordEncoder,
                                      Clock clock) {
        this.userReader = userReader;
        this.userStore = userStore;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public User signup(SignupCommand command) {
        if (userReader.existsByEmail(command.email())) {
            throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = new User(
                command.email(),
                passwordEncoder.encode(command.rawPassword()),
                command.name(),
                command.phone());
        userStore.save(user);
        return user;
    }

    @Transactional
    public User login(LoginCommand command) {
        User user = userReader.findByEmail(command.email())
                .filter(u -> passwordEncoder.matches(command.rawPassword(), u.getPasswordHash()))
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS));
        user.updateLastLoginAt(LocalDateTime.now(clock));
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<User> findUser(Long userId) {
        return userReader.findById(userId);
    }
}
