package com.personal.happygallery.adapter.out.external.payment;

import com.personal.happygallery.application.payment.port.out.PaymentPort;

/**
 * PG 결제/환불 인프라 인터페이스.
 * {@link PaymentPort}를 확장하여 infra 구현체가 app 포트를 자동으로 만족한다.
 * 구현체: {@link FakePaymentProvider} (개발·테스트), {@link TossPaymentsProvider} (프로덕션).
 * 보호 데코레이터: {@link ResilientPaymentProvider}.
 */
public interface PaymentProvider extends PaymentPort {
}
