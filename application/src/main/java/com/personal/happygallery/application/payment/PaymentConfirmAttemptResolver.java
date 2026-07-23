package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
class PaymentConfirmAttemptResolver {

    private final Map<PaymentContext, PaymentFulfiller> fulfillers;
    private final ObjectMapper objectMapper;
    private final FieldEncryptor fieldEncryptor;
    private final CompletedGuestAccessTokenResolver accessTokenResolver;

    PaymentConfirmAttemptResolver(List<PaymentFulfiller> fulfillers,
                                  ObjectMapper objectMapper,
                                  FieldEncryptor fieldEncryptor,
                                  CompletedGuestAccessTokenResolver accessTokenResolver) {
        this.fulfillers = new EnumMap<>(PaymentContext.class);
        for (PaymentFulfiller fulfiller : fulfillers) {
            PaymentContext context = fulfiller.context();
            if (this.fulfillers.put(context, fulfiller) != null) {
                throw new IllegalStateException("결제 확정 전략이 중복 등록되었습니다: " + context);
            }
        }
        this.objectMapper = objectMapper;
        this.fieldEncryptor = fieldEncryptor;
        this.accessTokenResolver = accessTokenResolver;
    }

    PreparedPaymentPayload readPayload(PaymentAttempt attempt) {
        String json = fieldEncryptor.decrypt(attempt.getPayloadEnc());
        return objectMapper.readValue(json, PreparedPaymentPayload.class);
    }

    void validateStoredPayload(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        fulfiller(attempt.getContext()).validateStoredPayload(attempt, payload);
    }

    PaymentFulfiller.FulfillResult fulfill(PaymentAttempt attempt, PreparedPaymentPayload payload) {
        return fulfiller(attempt.getContext()).fulfill(attempt, payload);
    }

    ConfirmResult confirmedResult(PaymentAttempt attempt) {
        if (attempt.getFulfilledDomainId() == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "완료된 결제 결과가 없습니다.");
        }
        CompletedGuestAccessTokenResolver.ResolvedAccess access = accessTokenResolver.resolve(attempt);
        return new ConfirmResult(
                attempt.getContext(),
                attempt.getFulfilledDomainId(),
                access.accessToken(),
                access.recoveryRequired());
    }

    private PaymentFulfiller fulfiller(PaymentContext context) {
        PaymentFulfiller fulfiller = fulfillers.get(context);
        if (fulfiller == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지원하지 않는 결제 컨텍스트입니다.");
        }
        return fulfiller;
    }
}
