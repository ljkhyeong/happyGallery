package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminNotificationController;
import com.personal.happygallery.adapter.in.web.admin.AdminPassController;
import com.personal.happygallery.adapter.in.web.admin.AdminPaymentReconciliationController;
import com.personal.happygallery.adapter.in.web.admin.AdminRefundController;
import com.personal.happygallery.adapter.in.web.admin.LocalEmailVerificationController;
import com.personal.happygallery.adapter.in.web.admin.LocalPhoneVerificationController;
import com.personal.happygallery.adapter.in.web.admin.LocalRefundFailureController;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.customer.port.in.DevPhoneVerificationQueryUseCase;
import com.personal.happygallery.application.customer.port.in.DevEmailVerificationQueryUseCase;
import com.personal.happygallery.application.notification.port.in.NotificationFailureAdminUseCase;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.application.payment.port.in.DevRefundFailureUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import com.personal.happygallery.application.payment.port.in.RefundQueryUseCase;
import com.personal.happygallery.application.payment.port.in.RefundRetryUseCase;
import com.personal.happygallery.application.search.dto.AdminPassStatus;
import com.personal.happygallery.application.search.dto.AdminPassView;
import com.personal.happygallery.application.search.port.in.AdminPassQueryUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.booking.Refund;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationOutbox;
import com.personal.happygallery.domain.notification.NotificationOutboxStatus;
import com.personal.happygallery.domain.notification.NotificationRecipientType;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import com.personal.happygallery.domain.payment.RefundStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminOperationsApiRestDocsTest extends RestDocsTestSupport {

    private static final String SNIPPET_GROUP = "admin-api-rest-docs-test";

    private MockMvc mockMvc;
    private RefundRetryUseCase refundRetryUseCase;
    private RefundQueryUseCase refundQueryUseCase;
    private NotificationFailureAdminUseCase notificationFailureAdminUseCase;
    private PaymentReconciliationAdminUseCase paymentReconciliationAdminUseCase;
    private PassExpiryBatchUseCase passExpiryBatchUseCase;
    private PassRefundUseCase passRefundUseCase;
    private AdminPassQueryUseCase adminPassQueryUseCase;
    private DevPhoneVerificationQueryUseCase phoneVerificationQueryUseCase;
    private DevEmailVerificationQueryUseCase emailVerificationQueryUseCase;
    private DevRefundFailureUseCase devRefundFailureUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        refundRetryUseCase = mock(RefundRetryUseCase.class);
        refundQueryUseCase = mock(RefundQueryUseCase.class);
        notificationFailureAdminUseCase = mock(NotificationFailureAdminUseCase.class);
        paymentReconciliationAdminUseCase = mock(PaymentReconciliationAdminUseCase.class);
        passExpiryBatchUseCase = mock(PassExpiryBatchUseCase.class);
        passRefundUseCase = mock(PassRefundUseCase.class);
        adminPassQueryUseCase = mock(AdminPassQueryUseCase.class);
        phoneVerificationQueryUseCase = mock(DevPhoneVerificationQueryUseCase.class);
        emailVerificationQueryUseCase = mock(DevEmailVerificationQueryUseCase.class);
        devRefundFailureUseCase = mock(DevRefundFailureUseCase.class);

        Refund orderRefund = RestDocsFixtures.orderRefund();
        Refund failedOrderClaimRefund = RestDocsFixtures.failedOrderClaimRefund();
        when(refundRetryUseCase.listFailed(isNull(), anyInt()))
                .thenReturn(new CursorPage<>(List.of(failedOrderClaimRefund), null, false));
        when(refundRetryUseCase.retry(anyLong())).thenReturn(orderRefund);
        when(refundQueryUseCase.getRefund(anyLong())).thenReturn(orderRefund);
        NotificationOutbox retriedNotification = mock(NotificationOutbox.class);
        when(retriedNotification.getId()).thenReturn(1L);
        when(retriedNotification.getRecipientType()).thenReturn(NotificationRecipientType.USER);
        when(retriedNotification.getUserId()).thenReturn(10L);
        when(retriedNotification.getEventType()).thenReturn(NotificationEventType.PASS_PURCHASED);
        when(retriedNotification.getAggregateType()).thenReturn("PASS");
        when(retriedNotification.getAggregateId()).thenReturn(300L);
        when(retriedNotification.getStatus()).thenReturn(NotificationOutboxStatus.PENDING);
        when(retriedNotification.getCreatedAt())
                .thenReturn(LocalDateTime.of(2026, 5, 1, 21, 0));
        when(notificationFailureAdminUseCase.listFailed()).thenReturn(List.of());
        when(notificationFailureAdminUseCase.retry(1L)).thenReturn(retriedNotification);
        when(paymentReconciliationAdminUseCase.listRequired()).thenReturn(List.of());
        when(paymentReconciliationAdminUseCase.reconcile(1L)).thenReturn(
                new PaymentReconciliationAdminUseCase.ReconciliationResult(
                        1L,
                        PaymentAttemptStatus.CONFIRMED,
                        300L,
                        "PG 승인 확인 후 서비스 처리를 완료했습니다."));
        when(passExpiryBatchUseCase.expireAll()).thenReturn(batchResult());
        when(passRefundUseCase.refundPass(300L))
                .thenReturn(new PassRefundUseCase.PassRefundResult(
                        1, 7, 210000L, 900L, RefundStatus.REQUESTED));
        AdminPassView adminPass = new AdminPassView(
                300L,
                "PASS-00000300",
                "홍길동",
                "01012345678",
                AdminPassStatus.ACTIVE,
                7,
                8,
                LocalDateTime.of(2026, 8, 1, 0, 0),
                1,
                280000L,
                null);
        when(adminPassQueryUseCase.search(any(), eq(0), eq(20)))
                .thenReturn(OffsetPage.of(List.of(adminPass), 0, 20, 1));
        when(adminPassQueryUseCase.get(300L)).thenReturn(adminPass);
        when(phoneVerificationQueryUseCase.findLatestUnverifiedCode(
                "01012345678", PhoneVerificationPurpose.GUEST_BOOKING))
                .thenReturn(Optional.of("123456"));
        when(emailVerificationQueryUseCase.findLatestUnverifiedCode(
                10L, "member@example.com"))
                .thenReturn(Optional.of("654321"));

        mockMvc = mockMvc(restDocumentation, SNIPPET_GROUP,
                new AdminRefundController(refundRetryUseCase, refundQueryUseCase),
                new AdminNotificationController(notificationFailureAdminUseCase),
                new AdminPaymentReconciliationController(paymentReconciliationAdminUseCase),
                new AdminPassController(passExpiryBatchUseCase, passRefundUseCase, adminPassQueryUseCase),
                new LocalPhoneVerificationController(phoneVerificationQueryUseCase),
                new LocalEmailVerificationController(emailVerificationQueryUseCase),
                new LocalRefundFailureController(devRefundFailureUseCase));
    }

    @Test
    @DisplayName("관리자 실패 환불 목록 API를 문서화한다")
    void admin_list_failed_refunds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/refunds/failed").with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].orderId").value(200L))
                .andExpect(jsonPath("$.content[0].orderClaimId").value(201L))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @DisplayName("관리자 환불 상태 조회 API를 문서화한다")
    void admin_get_refund() throws Exception {
        mockMvc.perform(get("/api/v1/admin/refunds/{refundId}", 901L).with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(901))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("관리자 환불 재시도 API를 문서화한다")
    void admin_retry_refund() throws Exception {
        mockMvc.perform(post("/api/v1/admin/refunds/{refundId}/retry", 1L).with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundId").value(901))
                .andExpect(jsonPath("$.status").value("REQUESTED"));
    }

    @Test
    @DisplayName("관리자 실패 알림 목록 API를 문서화한다")
    void admin_list_failed_notifications() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notifications/failed").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 실패 알림 재처리 API를 문서화한다")
    void admin_retry_notification() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notifications/{outboxId}/retry", 1L).with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("관리자 결제 대사 대상 목록 API를 문서화한다")
    void admin_list_payment_reconciliations() throws Exception {
        mockMvc.perform(get("/api/v1/admin/payment-attempts/reconciliation-required").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 결제 대사 실행 API를 문서화한다")
    void admin_reconcile_payment() throws Exception {
        mockMvc.perform(post("/api/v1/admin/payment-attempts/{attemptId}/reconcile", 1L)
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.domainId").value(300L));
    }

    @Test
    @DisplayName("관리자 8회권 검색 API를 문서화한다")
    void admin_search_passes() throws Exception {
        mockMvc.perform(get("/api/v1/admin/passes/search")
                        .with(adminUser())
                        .param("keyword", "01012345678")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].passNumber").value("PASS-00000300"));
    }

    @Test
    @DisplayName("관리자 8회권 상세 API를 문서화한다")
    void admin_get_pass() throws Exception {
        mockMvc.perform(get("/api/v1/admin/passes/{passId}", 300L).with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passId").value(300L));
    }

    @Test
    @DisplayName("관리자 8회권 만료 배치 API를 문서화한다")
    void admin_expire_passes() throws Exception {
        mockMvc.perform(post("/api/v1/admin/passes/expire").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 8회권 환불 API를 문서화한다")
    void admin_refund_pass() throws Exception {
        mockMvc.perform(post("/api/v1/admin/passes/{passId}/refund", 300L).with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로컬 최신 휴대폰 인증 코드 조회 API를 문서화한다")
    void local_latest_phone_verification_code() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dev/phone-verifications/latest")
                        .with(adminUser())
                        .param("phone", "01012345678")
                        .param("purpose", "GUEST_BOOKING"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로컬 최신 이메일 인증 코드 조회 API를 문서화한다")
    void local_latest_email_verification_code() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dev/email-verifications/latest")
                        .with(adminUser())
                        .param("userId", "10")
                        .param("email", "member@example.com"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로컬 다음 환불 실패 설정 API를 문서화한다")
    void local_arm_next_refund_failure() throws Exception {
        mockMvc.perform(post("/api/v1/admin/dev/payment/refunds/fail-next")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("{\"reason\":\"로컬 smoke 강제 환불 실패\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로컬 다음 환불 실패 해제 API를 문서화한다")
    void local_clear_next_refund_failure() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/dev/payment/refunds/fail-next").with(adminUser()))
                .andExpect(status().isNoContent());
    }

    private static BatchResult batchResult() {
        return new BatchResult(1, 0, Map.of());
    }
}
