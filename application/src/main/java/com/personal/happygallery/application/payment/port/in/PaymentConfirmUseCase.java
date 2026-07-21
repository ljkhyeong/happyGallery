package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.payment.PaymentContext;

/**
 * 결제 확정 유스케이스.
 *
 * <p>Toss 결제창 통과 후 프론트가 받은 paymentKey/orderId/amount를 서버로 보내면,
 * 서버가 {@link com.personal.happygallery.domain.payment.PaymentAttempt}와 amount 일치를 검증하고 선점한 뒤
 * DB 트랜잭션 밖에서 PG confirm을 호출한다. 성공 시 context별 fulfiller가 실제 도메인 저장을 수행하며,
 * PG 승인 후 로컬 저장 실패는 보상 환불로 연결한다.
 */
public interface PaymentConfirmUseCase {

    /**
     * confirm 입력. paymentKey가 null이면 amount=0 경로(예: 8회권 사용 예약)로 간주하고 PG 호출을 생략한다.
     * 고객 요청과 서버 내부 복구가 검증을 우회하는 의도를 호출부에서 명확히 드러내도록 생성 경로를 분리한다.
     */
    record ConfirmCommand(
            String paymentKey,
            String orderId,
            long amount,
            AuthContext auth,
            String statusToken,
            boolean trustedInternalRecovery
    ) {

        public static ConfirmCommand customerRequest(String paymentKey, String orderId, long amount,
                                                     AuthContext auth, String statusToken) {
            return new ConfirmCommand(paymentKey, orderId, amount, auth, statusToken, false);
        }

        public static ConfirmCommand trustedRecovery(String paymentKey, String orderId, long amount,
                                                     AuthContext auth) {
            return new ConfirmCommand(paymentKey, orderId, amount, auth, null, true);
        }
    }

    record ConfirmResult(
            PaymentContext context,
            Long domainId,
            String accessToken,
            boolean accessRecoveryRequired
    ) {}

    ConfirmResult confirm(ConfirmCommand command);
}
