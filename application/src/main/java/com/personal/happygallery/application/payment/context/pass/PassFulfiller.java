package com.personal.happygallery.application.payment.context.pass;

import com.personal.happygallery.application.pass.port.in.PassPurchaseUseCase;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedPassPayload;
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
    public void validateStoredPayload(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        if (!(payload instanceof PreparedPassPayload pp)) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "8회권 금액 정보가 없습니다. 결제를 다시 준비해 주세요.");
        }
        if (pp.userId() == null || pp.totalPrice() != attempt.getAmount() || pp.totalPrice() <= 0L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "저장된 8회권 금액이 결제 금액과 일치하지 않습니다.");
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public FulfillResult fulfill(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        PreparedPassPayload pp = (PreparedPassPayload) payload;
        PassPurchase purchase = passPurchaseUseCase.purchaseForMember(pp.userId(), pp.totalPrice());
        purchase.recordPaymentKey(attempt.getConfirmedPaymentKey());
        return new FulfillResult(purchase.getId(), null);
    }
}
