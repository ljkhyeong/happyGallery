package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminMfaRecoveryTransactionServiceTest {

    @Mock
    private AdminUserPort adminUserPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminMfaResetService mfaResetService;

    @InjectMocks
    private AdminMfaRecoveryTransactionService service;

    @DisplayName("복구 비밀번호 검증 후 공통 MFA 초기화 서비스에 복구 결과를 전달한다")
    @Test
    void recover_delegatesReset_afterPasswordIsVerified() {
        AdminUser admin = mock(AdminUser.class);
        given(adminUserPort.findByIdForUpdate(42L)).willReturn(Optional.of(admin));
        given(admin.isMfaEnabled()).willReturn(true);
        given(admin.getPasswordHash()).willReturn("password-hash");
        given(passwordEncoder.matches("current-password", "password-hash")).willReturn(true);

        service.recover(42L, "current-password");

        verify(mfaResetService).resetAfterRecovery(admin);
        verify(admin, never()).disableMfa();
    }
}
