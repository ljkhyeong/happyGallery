package com.personal.happygallery.application.payment;

import tools.jackson.databind.ObjectMapper;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.context.PaymentPreparer.PreparedPayment;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
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
    private final FieldEncryptor fieldEncryptor;

    public DefaultPaymentPrepareService(List<PaymentPreparer> preparers,
                                        PaymentAttemptStorePort attemptStore,
                                        ObjectMapper objectMapper,
                                        FieldEncryptor fieldEncryptor) {
        this.preparers = new EnumMap<>(PaymentContext.class);
        for (PaymentPreparer preparer : preparers) {
            PaymentContext context = preparer.context();
            if (this.preparers.put(context, preparer) != null) {
                throw new IllegalStateException("결제 준비 전략이 중복 등록되었습니다: " + context);
            }
        }
        this.attemptStore = attemptStore;
        this.objectMapper = objectMapper;
        this.fieldEncryptor = fieldEncryptor;
    }

    @Override
    public PrepareResult prepare(PrepareCommand command) {
        PaymentPreparer preparer = preparers.get(command.context());
        if (preparer == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지원하지 않는 결제 컨텍스트입니다.");
        }

        PreparedPayment prepared = preparer.prepare(command.payload(), command.auth());
        if (prepared.amount() < 0) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "결제 금액은 0 이상이어야 합니다.");
        }

        String orderIdExternal = UUID.randomUUID().toString();
        String payloadEnc = fieldEncryptor.encrypt(serialize(prepared.payload()));
        PaymentAttempt attempt = PaymentAttempt.start(
                orderIdExternal, command.context(), prepared.amount(), payloadEnc);
        attemptStore.save(attempt);

        return new PrepareResult(orderIdExternal, prepared.amount(), command.context());
    }

    private String serialize(Object payload) {
        return objectMapper.writeValueAsString(payload);
    }
}
