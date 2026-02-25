package com.personal.happygallery.infra.payment;

/**
 * PG 결제/환불 포트 (Port).
 * 구현체: {@link FakePaymentProvider} (개발·테스트), 실 PG 어댑터 (추후 §11+).
 */
public interface PaymentProvider {

    /**
     * 환불 실행.
     *
     * @param pgRef  원결제 PG 참조값 (없으면 null)
     * @param amount 환불 금액 (원)
     * @return 성공 시 success=true + pgRef, 실패 시 success=false + failReason
     */
    RefundResult refund(String pgRef, long amount);
}
