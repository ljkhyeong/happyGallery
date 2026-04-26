package com.personal.happygallery.application.payment.context;

import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.domain.payment.PaymentContext;

/**
 * context별 prepare 단계 실행자.
 *
 * <p>슬롯/상품 존재 여부 확인 같은 read-only 검증과 amount 산출만 담당한다.
 * 실제 도메인 상태 변경(슬롯 락, 인증 코드 소비, Guest upsert)은 {@link PaymentFulfiller}에서 수행한다.
 */
public interface PaymentPreparer {

    /** 어떤 PaymentContext를 처리하는 preparer인지. {@link com.personal.happygallery.application.payment.DefaultPaymentPrepareService}가 dispatch에 사용. */
    PaymentContext context();

    /**
     * payload + 인증 정보 검증 후 결제 amount(원, KRW)를 반환한다. 0이면 PG 호출 생략 경로.
     *
     * @param payload 클라이언트 입력 payload (sealed)
     * @param auth 호출자 인증 정보 (회원이면 userId, 비회원이면 null)
     */
    long calculateAmount(PaymentPayload payload, AuthContext auth);
}
