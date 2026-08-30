package com.personal.happygallery.application.payment.port.out;

/**
 * 외부 결제 시스템 연동 포트.
 *
 * <p>application 서비스는 이 포트만 의존하며, 실제 PG 구현은 adapter가 담당한다.
 */
public interface PaymentPort {

    /**
     * 결제 승인을 확정한다. 프론트에서 PG 결제창을 통과한 뒤 서버가 이 메서드로 최종 확정한다.
     *
     * @param paymentKey PG가 발급한 결제 키 (Toss paymentKey 등)
     * @param orderId    서버가 prepare 단계에 생성한 외부 주문 식별자 (UUID)
     * @param amount     확정 금액 (원) — prepare 단계 금액과 일치해야 한다
     * @param idempotencyKey 동일 승인 재시도에 계속 사용하는 PG 멱등키
     * @return 성공 시 success=true + paymentKey/method/approvedAt, 실패 시 success=false + failReason.
     *         외부 통신·서킷 브레이커·타임아웃 같은 운영 실패도 null이나 예외가 아니라 실패 결과로 반환한다.
     */
    PaymentConfirmResult confirm(String paymentKey, String orderId, long amount, String idempotencyKey);

    /** 저장된 orderId로 PG의 현재 승인 상태를 조회한다. 자동 판정 불가와 통신 장애를 구분해 반환한다. */
    PaymentLookupResult lookupByOrderId(String orderId);

    /**
     * 환불을 실행한다.
     *
     * @param paymentKey 원결제 paymentKey (없으면 null)
     * @param amount 환불 금액 (원)
     * @param idempotencyKey 최초 환불과 재시도에서 동일하게 사용하는 PG 멱등키
     * @return 성공, 최종 실패, 재시도 가능 실패, 상태 확인 필요 중 하나와 관련 상세값
     */
    RefundResult refund(String paymentKey, long amount, String idempotencyKey);

    /** 원결제 조회 응답의 취소 이력에서 해당 멱등키로 요청한 완료 환불을 확인한다. */
    RefundLookupResult lookupRefund(String paymentKey, long amount, String idempotencyKey);
}
