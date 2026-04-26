package com.personal.happygallery.application.payment.context.pass;

import com.personal.happygallery.application.pass.PassPriceProperties;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentContext;
import org.springframework.stereotype.Component;

@Component
public class PassPreparer implements PaymentPreparer {

    private final PassPriceProperties priceProperties;

    public PassPreparer(PassPriceProperties priceProperties) {
        this.priceProperties = priceProperties;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.PASS;
    }

    @Override
    public long calculateAmount(PaymentPayload payload, AuthContext auth) {
        if (!(payload instanceof PassPayload pp)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 결제 payload가 아닙니다.");
        }
        if (!auth.isMember() || pp.userId() == null || !pp.userId().equals(auth.userId())) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 구매는 회원 인증이 필요합니다.");
        }
        return priceProperties.totalPrice();
    }
}
