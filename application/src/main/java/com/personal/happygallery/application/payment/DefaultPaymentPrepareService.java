package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.context.PaymentPreparer.PreparedPayment;
import com.personal.happygallery.application.payment.port.in.PaymentPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PreparedBookingPayload;
import com.personal.happygallery.application.payment.port.in.PaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
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
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class DefaultPaymentPrepareService implements PaymentPrepareUseCase {

    private final Map<PaymentContext, PaymentPreparer> preparers;
    private final PaymentAttemptStorePort attemptStore;
    private final ObjectMapper objectMapper;
    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexKeyRing blindIndexKeyRing;
    private final GuestTokenService guestTokenService;
    private final PublicPaymentAvailabilityGuard paymentAvailabilityGuard;

    public DefaultPaymentPrepareService(List<PaymentPreparer> preparers,
                                        PaymentAttemptStorePort attemptStore,
                                        ObjectMapper objectMapper,
                                        FieldEncryptor fieldEncryptor,
                                        BlindIndexKeyRing blindIndexKeyRing,
                                        GuestTokenService guestTokenService,
                                        PublicPaymentAvailabilityGuard paymentAvailabilityGuard) {
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
        this.blindIndexKeyRing = blindIndexKeyRing;
        this.guestTokenService = guestTokenService;
        this.paymentAvailabilityGuard = paymentAvailabilityGuard;
    }

    @Override
    public PrepareResult prepare(PrepareCommand command) {
        paymentAvailabilityGuard.requireAvailable();
        PaymentPreparer preparer = preparers.get(command.context());
        if (preparer == null) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지원하지 않는 결제 컨텍스트입니다.");
        }

        String orderIdExternal = UUID.randomUUID().toString();
        PreparedPayment prepared = preparer.prepare(orderIdExternal, command.payload(), command.auth());

        String payloadJson = objectMapper.writeValueAsString(prepared.payload());
        String payloadEnc = fieldEncryptor.encrypt(payloadJson);
        GuestTokenService.IssuedToken issuedToken = command.auth().isMember()
                ? null
                : guestTokenService.issuePaymentStatusToken();
        PaymentAttempt attempt = command.auth().isMember()
                ? PaymentAttempt.startForMember(
                        orderIdExternal, command.context(), prepared.amount(), payloadEnc, command.auth().userId())
                : PaymentAttempt.startForGuest(
                        orderIdExternal, command.context(), prepared.amount(), payloadEnc,
                        blindIndexKeyRing.index(guestPhone(prepared.payload())),
                        blindIndexKeyRing.activeKeyId(),
                        issuedToken.tokenHash());
        attemptStore.save(attempt);

        return new PrepareResult(
                orderIdExternal,
                prepared.amount(),
                command.context(),
                issuedToken == null ? null : issuedToken.rawToken());
    }

    private String guestPhone(PaymentPayload payload) {
        return switch (payload) {
            case PreparedOrderPayload order -> order.phone();
            case PreparedBookingPayload booking -> booking.phone();
            default -> throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "비회원 결제 휴대폰 정보가 누락되었습니다.");
        };
    }
}
