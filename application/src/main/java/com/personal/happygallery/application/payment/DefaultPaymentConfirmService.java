package com.personal.happygallery.application.payment;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.personal.happygallery.application.payment.context.PaymentFulfiller;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptReaderPort;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.application.payment.port.out.PaymentConfirmResult;
import com.personal.happygallery.application.payment.port.out.PaymentPort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultPaymentConfirmService implements PaymentConfirmUseCase {

    private static final Logger log = LoggerFactory.getLogger(DefaultPaymentConfirmService.class);

    private final PaymentAttemptReaderPort attemptReader;
    private final PaymentAttemptStorePort attemptStore;
    private final PaymentPort paymentPort;
    private final Map<PaymentContext, PaymentFulfiller> fulfillers;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public DefaultPaymentConfirmService(PaymentAttemptReaderPort attemptReader,
                                        PaymentAttemptStorePort attemptStore,
                                        PaymentPort paymentPort,
                                        List<PaymentFulfiller> fulfillers,
                                        ObjectMapper objectMapper,
                                        Clock clock) {
        this.attemptReader = attemptReader;
        this.attemptStore = attemptStore;
        this.paymentPort = paymentPort;
        this.fulfillers = new EnumMap<>(PaymentContext.class);
        for (PaymentFulfiller fulfiller : fulfillers) {
            this.fulfillers.put(fulfiller.context(), fulfiller);
        }
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    public ConfirmResult confirm(ConfirmCommand command) {
        PaymentAttempt attempt = attemptReader.findByOrderIdExternal(command.orderId())
                .orElseThrow(() -> new NotFoundException("결제 시도"));
        attempt.requireConfirmable(command.amount());

        String paymentKey = command.paymentKey();
        String pgRef = null;
        if (attempt.getAmount() > 0) {
            if (paymentKey == null || paymentKey.isBlank()) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "paymentKey가 누락되었습니다.");
            }
            PaymentConfirmResult pg = paymentPort.confirm(paymentKey, command.orderId(), command.amount());
            if (!pg.success()) {
                attempt.markFailed();
                attemptStore.saveAndFlush(attempt);
                throw new HappyGalleryException(ErrorCode.PAYMENT_FAILED,
                        pg.failReason() != null ? pg.failReason() : "결제 확정에 실패했습니다.");
            }
            pgRef = pg.pgRef();
        } else {
            log.debug("amount=0 결제 — PG 호출 생략 [orderId={}, context={}]", command.orderId(), attempt.getContext());
        }

        PaymentFulfiller fulfiller = fulfillers.get(attempt.getContext());
        if (fulfiller == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지원하지 않는 결제 컨텍스트입니다.");
        }
        PaymentPayload payload = deserialize(attempt.getPayloadJson());
        PaymentFulfiller.FulfillResult fulfilled = fulfiller.fulfill(attempt, payload, command.auth());

        attempt.markConfirmed(paymentKey, pgRef, LocalDateTime.now(clock));
        attemptStore.saveAndFlush(attempt);

        return new ConfirmResult(attempt.getContext(), fulfilled.domainId(), fulfilled.rawAccessToken());
    }

    private PaymentPayload deserialize(String json) {
        try {
            return objectMapper.readValue(json, PaymentPayload.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("결제 payload 역직렬화 실패", e);
        }
    }
}
