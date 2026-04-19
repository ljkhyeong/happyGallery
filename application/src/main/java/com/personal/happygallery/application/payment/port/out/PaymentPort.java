package com.personal.happygallery.application.payment.port.out;

/**
 * 외부 결제 시스템 연동 포트.
 *
 * <p>application 서비스는 이 포트만 의존하며, 실제 PG 구현은 adapter가 담당한다.
 */
public interface PaymentPort {

    /**
     * 환불을 실행한다.
     *
     * @param pgRef  원결제 PG 참조값 (없으면 null)
     * @param amount 환불 금액 (원)
     * @return 성공 시 success=true + pgRef, 실패 시 success=false + failReason
     */
    RefundResult refund(String pgRef, long amount);
}
