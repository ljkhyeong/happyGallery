package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.payment.context.PaymentPreparer;
import com.personal.happygallery.application.payment.context.PaymentPreparer.PreparedPayment;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedBookingPayload;
import com.personal.happygallery.application.payment.context.PreparedPaymentPayload.PreparedOrderPayload;
import com.personal.happygallery.application.policy.PolicyConsentService;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.out.PaymentAttemptStorePort;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.crypto.BlindIndexKeyRing;
import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.policy.PolicyConsentPurpose;
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
    private final MemberAccountGuard memberAccountGuard;
    private final PolicyConsentService policyConsentService;

    public DefaultPaymentPrepareService(List<PaymentPreparer> preparers,
                                        PaymentAttemptStorePort attemptStore,
                                        ObjectMapper objectMapper,
                                        FieldEncryptor fieldEncryptor,
                                        BlindIndexKeyRing blindIndexKeyRing,
                                        GuestTokenService guestTokenService,
                                        PublicPaymentAvailabilityGuard paymentAvailabilityGuard,
                                        MemberAccountGuard memberAccountGuard,
                                        PolicyConsentService policyConsentService) {
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
        this.memberAccountGuard = memberAccountGuard;
        this.policyConsentService = policyConsentService;
    }

    @Override
    public PrepareResult prepare(PrepareCommand command) {
        paymentAvailabilityGuard.requireAvailable();
        if (command.auth().isMember()) {
            memberAccountGuard.requireActiveForUpdate(command.auth().userId());
        } else {
            policyConsentService.requireCurrent(command.payload().policyAcceptance());
        }
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
        PaymentAttempt savedAttempt = attemptStore.save(attempt);
        if (!command.auth().isMember()) {
            policyConsentService.recordForPaymentAttempt(
                    savedAttempt.getId(),
                    guestConsentPurpose(command.context()),
                    command.payload().policyAcceptance());
        }

        return new PrepareResult(
                orderIdExternal,
                prepared.amount(),
                command.context(),
                issuedToken == null ? null : issuedToken.rawToken());
    }

    private String guestPhone(PreparedPaymentPayload payload) {
        return switch (payload) {
            case PreparedOrderPayload order -> order.phone();
            case PreparedBookingPayload booking -> booking.phone();
            default -> throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "비회원 결제 휴대폰 정보가 누락되었습니다.");
        };
    }

    private PolicyConsentPurpose guestConsentPurpose(PaymentContext context) {
        return switch (context) {
            case ORDER -> PolicyConsentPurpose.GUEST_ORDER_PAYMENT;
            case BOOKING -> PolicyConsentPurpose.GUEST_BOOKING_PAYMENT;
            case PASS -> throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT, "비회원은 8회권을 구매할 수 없습니다.");
        };
    }
}
