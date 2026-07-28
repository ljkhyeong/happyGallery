package com.personal.happygallery.application.payment.context;

import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.domain.payment.PaymentContext;

/**
 * context별 prepare 단계 실행자.
 *
 * <p>슬롯/상품 존재 여부 확인, amount 산출과 confirm에서 사용할 서버 확정 payload 생성을 담당한다.
 * 비회원 주문·예약은 이 단계에서 인증 코드를 소비하고 결제 시도에 귀속된 서명 증거를 저장한다.
 * 실제 주문·예약 생성과 Guest upsert는 {@link PaymentFulfiller}에서 수행한다.
 */
public interface PaymentPreparer {

    /** 어떤 PaymentContext를 처리하는 preparer인지. 결제 준비 트랜잭션 서비스가 dispatch에 사용한다. */
    PaymentContext context();

    /**
     * payload + 인증 정보 검증 후 결제 amount와 서버 확정 payload를 반환한다.
     * amount가 0이면 PG 호출 생략 경로다.
     *
     * @param paymentOrderId 이번 결제 시도를 식별하는 외부 주문번호
     * @param payload 클라이언트 입력 payload (sealed)
     * @param auth 호출자 인증 정보 (회원이면 userId, 비회원이면 null)
     */
    PreparedPayment prepare(String paymentOrderId, PaymentPayload payload, AuthContext auth);

    record PreparedPayment(long amount, PreparedPaymentPayload payload) {}
}
