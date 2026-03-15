package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.UserSession;
import com.personal.happygallery.infra.user.UserRepository;
import com.personal.happygallery.infra.user.UserSessionRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerAuthService {

    static final long SESSION_TTL_DAYS = 7;

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final Clock clock;

    public CustomerAuthService(UserRepository userRepository,
                               UserSessionRepository sessionRepository,
                               Clock clock) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    @Transactional
    public TokenResult signup(String email, String rawPassword, String name, String phone) {
        if (userRepository.existsByEmail(email)) {
            throw new HappyGalleryException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = new User(email, passwordEncoder.encode(rawPassword), name, phone);
        userRepository.save(user);

        return createSession(user);
    }

    @Transactional
    public TokenResult login(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.INVALID_CREDENTIALS));

        user.updateLastLoginAt(LocalDateTime.now(clock));

        return createSession(user);
    }

    @Transactional
    public void logout(String rawToken) {
        String hash = hashToken(rawToken);
        sessionRepository.findBySessionTokenHash(hash)
                .ifPresent(session -> sessionRepository.delete(session));
    }

    @Transactional(readOnly = true)
    public Optional<User> validateSession(String rawToken) {
        String hash = hashToken(rawToken);
        return sessionRepository.findBySessionTokenHash(hash)
                .filter(session -> !session.isExpired(LocalDateTime.now(clock)))
                .flatMap(session -> userRepository.findById(session.getUserId()));
    }

    private TokenResult createSession(User user) {
        String rawToken = UUID.randomUUID().toString();
        String hash = hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now(clock).plusDays(SESSION_TTL_DAYS);

        UserSession session = new UserSession(user.getId(), hash, expiresAt);
        sessionRepository.save(session);

        return new TokenResult(rawToken, user);
    }

    static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record TokenResult(String rawToken, User user) {}
}
