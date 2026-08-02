package com.personal.happygallery.application.payment.port.out;

import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentAttemptStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentAttemptReaderPort {
    Optional<PaymentAttempt> findById(Long id);

    Optional<PaymentAttempt> findByIdForUpdate(Long id);

    /** Toss 응답의 orderId(=order_id_external, UUID 문자열)로 prepare 레코드를 조회한다. */
    Optional<PaymentAttempt> findByOrderIdExternal(String orderIdExternal);

    Optional<PaymentAttempt> findByOrderIdExternalForUpdate(String orderIdExternal);

    /** 회원 탈퇴를 막아야 하는 비종결 결제 시도가 있는지 조회한다. */
    boolean existsNonTerminalByOwnerUserId(Long userId);

    /** 휴대폰 소유 확인으로 복구할 결제 시도를 교착 방지를 위해 ID 순서로 잠근다. */
    List<PaymentAttempt> findGuestRecoveryCandidatesForUpdate(
            List<String> phoneHmacCandidates,
            List<PaymentAttemptStatus> terminalStatuses,
            LocalDateTime terminalCutoff);

    /** confirm 도중 중단된 PROCESSING/RETRYABLE/APPROVED 시도 ID를 오래된 순서로 조회한다. */
    List<Long> findConfirmRecoveryCandidateIds(LocalDateTime activityStaleBefore,
                                                LocalDateTime createdAtStaleBeforeUtc,
                                                int limit);

    /** 아직 confirm을 시작하지 않은 채 유효시간이 지난 결제 준비 ID를 ID 키셋 순서로 조회한다. */
    List<Long> findExpiredPendingIdsAfterId(
            LocalDateTime createdBefore, Long afterId, int limit);

    /** 보존 기간이 지난 최종 상태 중 개인정보 암호문이 남은 결제 시도 ID를 조회한다. */
    List<Long> findSensitiveDataCleanupCandidateIds(
            LocalDateTime createdBefore, Long afterId, int limit);

    /** PG 조회를 통한 운영 대사가 필요한 결제 시도를 오래된 순서로 조회한다. */
    List<PaymentAttempt> findReconciliationRequired(int limit);

    /** 운영 대사가 필요한 결제 건수와 가장 오래된 마지막 confirm 실행 시각을 집계한다. */
    PaymentAttemptBacklogSummary summarizeReconciliationRequiredBacklog();
}
