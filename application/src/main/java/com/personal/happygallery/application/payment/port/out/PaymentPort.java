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
     * @return 성공 시 success=true + pgRef/method/approvedAt, 실패 시 success=false + failReason
     */
    PaymentConfirmResult confirm(String paymentKey, String orderId, long amount);

    /**
     * 환불을 실행한다.
     *
     * @param pgRef  원결제 PG 참조값 (없으면 null)
     * @param amount 환불 금액 (원)
     * @return 성공 시 success=true + pgRef, 실패 시 success=false + failReason
     */
    RefundResult refund(String pgRef, long amount);
}
