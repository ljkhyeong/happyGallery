package com.personal.happygallery.application.payment.context.pass;

import com.personal.happygallery.application.pass.port.in.PassPurchaseUseCase;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PassPayload;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PassFulfiller implements PaymentFulfiller {

    private final PassPurchaseUseCase passPurchaseUseCase;

    public PassFulfiller(PassPurchaseUseCase passPurchaseUseCase) {
        this.passPurchaseUseCase = passPurchaseUseCase;
    }

    @Override
    public PaymentContext context() {
        return PaymentContext.PASS;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentAttempt attempt, PaymentPayload payload, AuthContext auth, String pgRef) {
        if (!(payload instanceof PassPayload pp)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 결제 payload가 아닙니다.");
        }
        if (!auth.isMember() || pp.userId() == null || !pp.userId().equals(auth.userId())) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "8회권 구매는 회원 인증이 필요합니다.");
        }
        PassPurchase purchase = passPurchaseUseCase.purchaseForMember(auth.userId());
        purchase.recordPaymentKey(pgRef);
        return new FulfillResult(purchase.getId(), null);
    }
}
