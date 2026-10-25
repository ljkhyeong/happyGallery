package com.personal.happygallery.application.order;

import com.personal.happygallery.application.order.port.out.FulfillmentPort;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider.RegistrationCommand;
import com.personal.happygallery.application.order.port.out.ShipmentTrackingProvider.RegistrationResult;
import com.personal.happygallery.domain.order.Fulfillment;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배송조회 등록의 선점과 결과 저장.
 *
 * <p>외부 등록 호출은 두 트랜잭션 사이에서 트랜잭션 없이 실행된다. 선점이 호출 전에 커밋되고 호출 중 잠금을
 * 유지하지 않도록 {@code REQUIRES_NEW}를 사용한다(ADR-0013).
 */
@Service
class ShipmentTrackingRegistrationTransactionService {

    static final int MAX_ATTEMPTS = 10;
    /** 처리 중(PROCESSING) 상태로 이 시간 이상 머문 등록은 중단된 것으로 보고 다시 후보로 삼는다. */
    static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(5);

    private final FulfillmentPort fulfillmentPort;

    ShipmentTrackingRegistrationTransactionService(FulfillmentPort fulfillmentPort) {
        this.fulfillmentPort = fulfillmentPort;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<RegistrationCommand> claim(Long fulfillmentId, LocalDateTime now) {
        Fulfillment fulfillment = fulfillmentPort.findByIdForUpdate(fulfillmentId).orElse(null);
        if (fulfillment == null
                || !fulfillment.claimTrackingRegistration(now, now.minus(PROCESSING_TIMEOUT))) {
            return Optional.empty();
        }
        return Optional.of(new RegistrationCommand(
                fulfillment.getOrderId(), fulfillment.getCarrierCode(), fulfillment.getTrackingNumber()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(Long fulfillmentId, RegistrationResult result, LocalDateTime now) {
        Fulfillment fulfillment = fulfillmentPort.findByIdForUpdate(fulfillmentId).orElse(null);
        if (fulfillment == null) {
            return;
        }
        if (result.success()) {
            fulfillment.completeTrackingRegistration(result.requestId(), now);
        } else {
            LocalDateTime retryAt = result.retryable()
                    ? now.plusMinutes(retryDelayMinutes(fulfillment.getTrackingRegistrationAttempts()))
                    : null;
            fulfillment.failTrackingRegistration(result.reason(), retryAt, MAX_ATTEMPTS);
        }
    }

    private static long retryDelayMinutes(int attempts) {
        return Math.min(60, 1L << Math.min(attempts - 1, 6));
    }
}
