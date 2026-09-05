package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.application.customer.port.in.PhoneOwnershipVerificationUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort.LoginSnapshot;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.policy.PolicyConsentService;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCustomerAuthServiceTest {

    @Mock UserReaderPort userReader;
    @Mock UserStorePort userStore;
    @Mock PhoneOwnershipVerificationUseCase phoneOwnershipVerification;
    @Mock PasswordEncoder passwordEncoder;
    @Mock PolicyConsentService policyConsentService;

    @DisplayName("없는 계정과 소셜 전용 계정도 로컬 계정과 같은 BCrypt 비교를 한 번 수행한다")
    @Test
    void login_usesDummyBcryptForAccountsWithoutLocalPassword() {
        DefaultCustomerAuthService service = createService();
        when(userReader.findLoginSnapshotByEmail("missing@example.com"))
                .thenReturn(Optional.empty());
        when(userReader.findLoginSnapshotByEmail("social@example.com"))
                .thenReturn(Optional.of(new LoginSnapshot(1L, null, true)));

        assertInvalidCredentials(service, "missing@example.com");
        assertInvalidCredentials(service, "social@example.com");

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder, times(2)).matches(
                eq("wrong-password"),
                hashCaptor.capture());
        assertThat(hashCaptor.getAllValues())
                .allSatisfy(hash -> assertThat(hash).startsWith("$2a$"))
                .containsOnly(hashCaptor.getAllValues().getFirst());
        verify(userReader, never()).findByIdForUpdate(anyLong());
    }

    @DisplayName("비밀번호가 틀리면 회원 행을 잠그지 않는다")
    @Test
    void login_doesNotLockUserWhenPasswordDoesNotMatch() {
        DefaultCustomerAuthService service = createService();
        when(userReader.findLoginSnapshotByEmail("local@example.com"))
                .thenReturn(Optional.of(new LoginSnapshot(2L, "stored-hash", true)));
        when(passwordEncoder.matches("wrong-password", "stored-hash")).thenReturn(false);

        assertInvalidCredentials(service, "local@example.com");

        verify(passwordEncoder).matches("wrong-password", "stored-hash");
        verify(userReader, never()).findByIdForUpdate(2L);
    }

    @DisplayName("비밀번호 확인 뒤 회원을 잠그고 로그인 시각과 낮은 강도의 해시를 갱신한다")
    @Test
    void login_locksAfterPasswordMatchAndUpdatesLockedUser() {
        Clock clock = Clock.fixed(Instant.parse("2026-07-26T03:00:00Z"), ZoneOffset.UTC);
        DefaultCustomerAuthService service = createService(clock);
        User lockedUser = new User(
                "local@example.com",
                "stored-hash",
                "로컬 회원",
                "01012345678");
        when(userReader.findLoginSnapshotByEmail("local@example.com"))
                .thenReturn(Optional.of(new LoginSnapshot(2L, "stored-hash", true)));
        when(passwordEncoder.matches("correct-password", "stored-hash")).thenReturn(true);
        when(userReader.findByIdForUpdate(2L)).thenReturn(Optional.of(lockedUser));
        when(passwordEncoder.upgradeEncoding("stored-hash")).thenReturn(true);
        when(passwordEncoder.encode("correct-password")).thenReturn("upgraded-hash");

        User loggedInUser = service.login(new CustomerAuthUseCase.LoginCommand(
                "local@example.com",
                "correct-password"));

        assertThat(loggedInUser).isSameAs(lockedUser);
        assertThat(loggedInUser.getPasswordHash()).isEqualTo("upgraded-hash");
        assertThat(loggedInUser.getLastLoginAt())
                .isEqualTo(LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
        InOrder verificationOrder = inOrder(userReader, passwordEncoder);
        verificationOrder.verify(userReader).findLoginSnapshotByEmail("local@example.com");
        verificationOrder.verify(passwordEncoder).matches("correct-password", "stored-hash");
        verificationOrder.verify(userReader).findByIdForUpdate(2L);
    }

    @DisplayName("잠금 전후 비밀번호 해시가 바뀌면 최신 해시로 다시 검증한다")
    @Test
    void login_rechecksChangedPasswordHashAfterLock() {
        DefaultCustomerAuthService service = createService();
        User lockedUser = new User(
                "local@example.com",
                "changed-hash",
                "로컬 회원",
                "01012345678");
        when(userReader.findLoginSnapshotByEmail("local@example.com"))
                .thenReturn(Optional.of(new LoginSnapshot(2L, "observed-hash", true)));
        when(passwordEncoder.matches("old-password", "observed-hash")).thenReturn(true);
        when(userReader.findByIdForUpdate(2L)).thenReturn(Optional.of(lockedUser));
        when(passwordEncoder.matches("old-password", "changed-hash")).thenReturn(false);

        assertInvalidCredentials(service, "local@example.com", "old-password");

        verify(passwordEncoder).matches("old-password", "changed-hash");
        assertThat(lockedUser.getLastLoginAt()).isNull();
    }

    @DisplayName("다른 로그인 요청이 먼저 BCrypt 강도를 높여도 같은 비밀번호면 로그인을 계속한다")
    @Test
    void login_acceptsConcurrentHashUpgradeForSamePassword() {
        DefaultCustomerAuthService service = createService();
        User lockedUser = new User(
                "local@example.com",
                "upgraded-hash",
                "로컬 회원",
                "01012345678");
        when(userReader.findLoginSnapshotByEmail("local@example.com"))
                .thenReturn(Optional.of(new LoginSnapshot(2L, "observed-hash", true)));
        when(passwordEncoder.matches("correct-password", "observed-hash")).thenReturn(true);
        when(userReader.findByIdForUpdate(2L)).thenReturn(Optional.of(lockedUser));
        when(passwordEncoder.matches("correct-password", "upgraded-hash")).thenReturn(true);

        User loggedInUser = service.login(new CustomerAuthUseCase.LoginCommand(
                "local@example.com",
                "correct-password"));

        assertThat(loggedInUser).isSameAs(lockedUser);
        assertThat(loggedInUser.getLastLoginAt()).isNotNull();
        verify(passwordEncoder).matches("correct-password", "upgraded-hash");
    }

    private DefaultCustomerAuthService createService() {
        return createService(Clock.systemUTC());
    }

    private DefaultCustomerAuthService createService(Clock clock) {
        CustomerAuthenticationTransactionService authenticationService =
                new CustomerAuthenticationTransactionService(userReader, passwordEncoder, clock);
        return new DefaultCustomerAuthService(
                userReader,
                userStore,
                phoneOwnershipVerification,
                passwordEncoder,
                policyConsentService,
                authenticationService);
    }

    private static void assertInvalidCredentials(DefaultCustomerAuthService service, String email) {
        assertInvalidCredentials(service, email, "wrong-password");
    }

    private static void assertInvalidCredentials(DefaultCustomerAuthService service,
                                                 String email,
                                                 String password) {
        assertThatThrownBy(() -> service.login(
                new CustomerAuthUseCase.LoginCommand(
                        email,
                        password)))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.INVALID_CREDENTIALS));
    }
}
