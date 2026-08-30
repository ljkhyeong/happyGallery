package com.personal.happygallery.application.admin;

import com.personal.happygallery.application.admin.port.out.AdminMfaRecoveryCodePort;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminAuthOutcome;
import com.personal.happygallery.domain.admin.AdminUser;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminMfaResetServiceTest {

    @Mock
    private AdminUserPort adminUserPort;

    @Mock
    private AdminMfaRecoveryCodePort recoveryCodePort;

    @Mock
    private AdminAuthAuditService auditService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AdminMfaResetService service;

    @DisplayName("MFA 초기화는 자격 증명 버전을 올리고 복구 코드와 세션을 무효화한다")
    @Test
    void resetAfterDisable_disablesMfaAndPublishesCredentialsChangedEvent() {
        AdminUser admin = mock(AdminUser.class);
        given(admin.getId()).willReturn(42L);
        given(admin.getUsername()).willReturn("admin");
        given(admin.disableMfa()).willReturn(7L);

        service.resetAfterDisable(admin);

        InOrder order = inOrder(
                admin, recoveryCodePort, adminUserPort, auditService, eventPublisher);
        order.verify(admin).disableMfa();
        order.verify(recoveryCodePort).deleteByAdminUserId(42L);
        order.verify(adminUserPort).save(admin);
        order.verify(auditService).record(42L, "admin", AdminAuthOutcome.MFA_DISABLED);
        order.verify(eventPublisher).publishEvent(new AdminCredentialsChangedEvent(42L, 7L));
    }

    @DisplayName("MFA 복구 초기화는 복구 감사 결과를 기록한다")
    @Test
    void resetAfterRecovery_recordsRecoveryOutcome() {
        AdminUser admin = mock(AdminUser.class);
        given(admin.getId()).willReturn(42L);
        given(admin.getUsername()).willReturn("admin");
        given(admin.disableMfa()).willReturn(7L);

        service.resetAfterRecovery(admin);

        verify(auditService).record(42L, "admin", AdminAuthOutcome.MFA_RECOVERY_RESET);
    }

    @DisplayName("모든 MFA 초기화 진입점은 활성 트랜잭션 안에서만 실행한다")
    @Test
    void resetEntryPoints_requireActiveTransaction() throws NoSuchMethodException {
        for (String methodName : List.of("resetAfterDisable", "resetAfterRecovery")) {
            Method reset = AdminMfaResetService.class.getDeclaredMethod(
                    methodName, AdminUser.class);
            Transactional transactional = reset.getAnnotation(Transactional.class);

            assertThat(transactional).as(methodName).isNotNull();
            assertThat(transactional.propagation())
                    .as(methodName)
                    .isEqualTo(Propagation.MANDATORY);
        }
    }
}
