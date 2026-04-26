package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.payment.PaymentContext;

/**
 * 결제 확정 유스케이스.
 *
 * <p>Toss 결제창 통과 후 프론트가 받은 paymentKey/orderId/amount를 서버로 보내면,
 * 서버가 {@link com.personal.happygallery.domain.payment.PaymentAttempt}와 amount 일치를 검증한 뒤
 * {@link com.personal.happygallery.application.payment.port.out.PaymentPort#confirm(String, String, long)}을 호출하고,
 * 성공 시 context별 fulfiller가 실제 도메인 저장을 수행한다.
 */
public interface PaymentConfirmUseCase {

    /**
     * confirm 입력. paymentKey가 null이면 amount=0 경로(예: 8회권 사용 예약)로 간주하고 PG 호출을 생략한다.
     */
    record ConfirmCommand(String paymentKey, String orderId, long amount, AuthContext auth) {}

    record ConfirmResult(PaymentContext context, Long domainId, String accessToken) {}

    ConfirmResult confirm(ConfirmCommand command);
}
