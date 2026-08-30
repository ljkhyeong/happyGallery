package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@UseCaseIT
class CustomerLoginTransactionUseCaseIT {

    @Autowired CustomerAuthUseCase customerAuthUseCase;
    @Autowired UserReaderPort userReader;
    @Autowired UserStorePort userStore;
    @Autowired TestCleanupSupport cleanupSupport;
    @MockitoSpyBean PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        cleanupSupport.clearUsers();
    }

    @DisplayName("없는 회원의 dummy BCrypt 비교는 DB 트랜잭션 밖에서 수행한다")
    @Test
    void login_missingUser_comparesDummyHashOutsideTransaction() {
        List<Boolean> transactionStates = captureMatchTransactionStates();

        assertThatThrownBy(() -> customerAuthUseCase.login(
                new CustomerAuthUseCase.LoginCommand(
                        "missing-login@example.com",
                        "wrong-password")))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertSoftly(softly -> {
                            softly.assertThat(exception.getErrorCode())
                                    .isEqualTo(ErrorCode.INVALID_CREDENTIALS);
                            softly.assertThat(transactionStates).containsExactly(false);
                        }));
    }

    @DisplayName("회원의 최초 BCrypt는 트랜잭션 밖에서 수행하고 성공 변경은 잠금 트랜잭션에 저장한다")
    @Test
    void login_validUser_comparesFirstHashOutsideTransactionAndPersistsLogin() {
        String passwordHash = passwordEncoder.encode("correct-password");
        User savedUser = userStore.save(new User(
                "login-transaction@example.com",
                passwordHash,
                "로그인 회원",
                "01012345678"));
        List<Boolean> transactionStates = captureMatchTransactionStates();

        User loggedInUser = customerAuthUseCase.login(
                new CustomerAuthUseCase.LoginCommand(
                        "login-transaction@example.com",
                        "correct-password"));
        User persistedUser = userReader.findById(savedUser.getId()).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(transactionStates).containsExactly(false);
            softly.assertThat(loggedInUser.getId()).isEqualTo(savedUser.getId());
            softly.assertThat(loggedInUser.getLastLoginAt()).isNotNull();
            softly.assertThat(persistedUser.getLastLoginAt())
                    .isEqualTo(loggedInUser.getLastLoginAt());
        });
    }

    private List<Boolean> captureMatchTransactionStates() {
        List<Boolean> transactionStates = new ArrayList<>();
        doAnswer(invocation -> {
            transactionStates.add(
                    TransactionSynchronizationManager.isActualTransactionActive());
            return invocation.callRealMethod();
        }).when(passwordEncoder).matches(anyString(), anyString());
        return transactionStates;
    }
}
