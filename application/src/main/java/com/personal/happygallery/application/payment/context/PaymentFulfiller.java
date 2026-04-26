package com.personal.happygallery.application.payment.context;

import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;

/**
 * context별 confirm 단계 도메인 저장 실행자.
 *
 * <p>PG confirm 성공이 확정된 뒤에만 호출된다. 슬롯 락, 인증 코드 소비, Guest upsert,
 * 재고 차감, 도메인 저장, 알림 이벤트 발행을 모두 이 단계에서 수행한다.
 */
public interface PaymentFulfiller {

    /** 처리하는 PaymentContext. */
    PaymentContext context();

    /**
     * payload를 도메인 저장으로 풀어낸다.
     *
     * @param attempt 확정 직전 상태의 PaymentAttempt — 호출 전 amount/상태 검증 완료
     * @param payload 역직렬화된 prepare payload
     * @param auth 호출자 인증 정보 (payload와 일치 여부는 fulfiller가 검증)
     * @param pgRef PG 원결제 참조값. amount=0 경로는 null.
     * @return 생성된 도메인 ID + 비회원 access token (있으면)
     */
    FulfillResult fulfill(PaymentAttempt attempt, PaymentPayload payload, AuthContext auth, String pgRef);

    record FulfillResult(Long domainId, String rawAccessToken) {}
}
