package com.personal.happygallery.app.payment;

import com.personal.happygallery.app.payment.port.out.PaymentPort;
import com.personal.happygallery.app.payment.port.out.RefundResult;
import com.personal.happygallery.infra.payment.PaymentProvider;
import org.springframework.stereotype.Component;

/**
 * {@link PaymentProvider}(infra) → {@link PaymentPort}(app) 브릿지 어댑터.
 *
 * <p>점진적 헥사고날 전환 중 app 서비스가 infra 구현을 직접 참조하지 않도록 중개한다.
 * 향후 infra 모듈이 app 포트를 직접 구현하게 되면 이 클래스는 제거된다.
 */
@Component
public class PaymentPortAdapter implements PaymentPort {

    private final PaymentProvider delegate;

    public PaymentPortAdapter(PaymentProvider delegate) {
        this.delegate = delegate;
    }

    @Override
    public RefundResult refund(String pgRef, long amount) {
        var result = delegate.refund(pgRef, amount);
        return new RefundResult(result.success(), result.pgRef(), result.failReason());
    }
}
