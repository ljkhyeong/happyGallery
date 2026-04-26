package com.personal.happygallery.application.payment;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPaymentPrepareService implements PaymentPrepareUseCase {

    private final Map<PaymentContext, PaymentPreparer> preparers;
    private final PaymentAttemptStorePort attemptStore;
    private final ObjectMapper objectMapper;

    public DefaultPaymentPrepareService(List<PaymentPreparer> preparers,
                                        PaymentAttemptStorePort attemptStore,
                                        ObjectMapper objectMapper) {
        this.preparers = new EnumMap<>(PaymentContext.class);
        for (PaymentPreparer preparer : preparers) {
            this.preparers.put(preparer.context(), preparer);
        }
        this.attemptStore = attemptStore;
        this.objectMapper = objectMapper;
    }

    @Override
    public PrepareResult prepare(PrepareCommand command) {
        PaymentPreparer preparer = preparers.get(command.context());
        if (preparer == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지원하지 않는 결제 컨텍스트입니다.");
        }

        long amount = preparer.calculateAmount(command.payload(), command.auth());
        if (amount < 0) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "결제 금액은 0 이상이어야 합니다.");
        }

        String orderIdExternal = UUID.randomUUID().toString();
        String payloadJson = serialize(command.payload());
        PaymentAttempt attempt = PaymentAttempt.start(orderIdExternal, command.context(), amount, payloadJson);
        attemptStore.save(attempt);

        return new PrepareResult(orderIdExternal, amount, command.context());
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("결제 payload 직렬화 실패", e);
        }
    }
}
