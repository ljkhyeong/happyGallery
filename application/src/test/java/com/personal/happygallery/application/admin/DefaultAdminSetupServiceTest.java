package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultAdminSetupServiceTest {

    @Mock
    private AdminUserPort adminUserPort;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DefaultAdminSetupService adminSetupService;

    @DisplayName("관리자 계정이 없으면 최초 관리자 계정을 생성한다")
    @Test
    void setup_createsInitialAdmin_whenNoAdminExists() {
        given(adminUserPort.count()).willReturn(0L);
        given(adminUserPort.findByUsername("admin")).willReturn(Optional.empty());
        given(passwordEncoder.encode("admin123456")).willReturn("encoded-password");
        given(adminUserPort.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        adminSetupService.setup("admin", "admin123456");

        ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
        verify(adminUserPort).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo("admin");
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-password");
    }

    @DisplayName("이미 관리자 계정이 있으면 최초 관리자 계정 생성을 막는다")
    @Test
    void setup_throwsNotFound_whenAdminAlreadyExists() {
        given(adminUserPort.count()).willReturn(1L);

        assertThatThrownBy(() -> adminSetupService.setup("admin", "admin123456"))
                .isInstanceOf(HappyGalleryException.class)
                .extracting(ex -> ((HappyGalleryException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
        verify(adminUserPort, never()).save(any());
    }
}
