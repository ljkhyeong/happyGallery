package com.personal.happygallery.application.payment.port.in;

import com.personal.happygallery.domain.payment.PaymentContext;

/**
 * 결제 준비 유스케이스.
 *
 * <p>prepare 단계에서 서버는 orderIdExternal(UUID)과 amount를 확정해 {@link com.personal.happygallery.domain.payment.PaymentAttempt}를
 * {@link com.personal.happygallery.domain.payment.PaymentAttemptStatus#PENDING}으로 저장한다.
 * 프론트는 반환된 orderId/amount로 Toss 결제창을 호출한다.
 *
 * <p>amount 산출은 context별 preparer가 수행하며, 클라이언트가 금액을 전송하더라도
 * 서버 산출값만 저장된다 (변조 방어).
 */
public interface PaymentPrepareUseCase {

    /** prepare 입력. context별 payload는 JSON 역직렬화를 위해 별도 record로 분기된다. */
    record PrepareCommand(PaymentContext context, PaymentPayload payload, AuthContext auth) {}

    /**
     * prepare 결과. amount가 0이면 PG 호출 없이 바로 {@link PaymentConfirmUseCase}를 호출해도 된다 (8회권 사용 예약 등).
     */
    record PrepareResult(String orderId, long amount, PaymentContext context) {}

    PrepareResult prepare(PrepareCommand command);
}
