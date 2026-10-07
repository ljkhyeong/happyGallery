package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.PhoneVerificationConsumptionService;
import com.personal.happygallery.application.token.GuestTokenService;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 비회원 결제 준비에서 소비한 휴대폰 인증을 결제 시도에 귀속시키는 서명 증거를 관리한다. */
@Component
public class GuestPaymentVerificationService {

    private static final String VERSION = "v1";

    private final PhoneVerificationConsumptionService phoneVerificationConsumption;
    private final GuestTokenService guestTokenService;

    public GuestPaymentVerificationService(
            PhoneVerificationConsumptionService phoneVerificationConsumption,
            GuestTokenService guestTokenService) {
        this.phoneVerificationConsumption = phoneVerificationConsumption;
        this.guestTokenService = guestTokenService;
    }

    public String consumeAndIssue(
            PaymentContext context,
            String paymentOrderId,
            String phone,
            String verificationCode) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        phoneVerificationConsumption.consume(
                normalizedPhone, verificationCode, verificationPurpose(context));

        String nonce = UUID.randomUUID().toString();
        String signature = guestTokenService.signPayload(
                canonical(context, paymentOrderId, normalizedPhone, nonce));
        return String.join(".", VERSION, nonce, signature);
    }

    public void requireValid(
            PaymentContext context,
            String paymentOrderId,
            String phone,
            String proof) {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        String[] parts = proof == null ? new String[0] : proof.split("\\.", -1);
        if (parts.length != 3 || !VERSION.equals(parts[0]) || !isUuid(parts[1])) {
            throw invalidProof();
        }

        String canonical = canonical(context, paymentOrderId, normalizedPhone, parts[1]);
        if (!guestTokenService.matchesPayloadSignature(canonical, parts[2])) {
            throw invalidProof();
        }
    }

    private String canonical(
            PaymentContext context,
            String paymentOrderId,
            String normalizedPhone,
            String nonce) {
        if (context == null || paymentOrderId == null || paymentOrderId.isBlank()) {
            throw invalidProof();
        }
        return String.join("\n", VERSION, context.name(), paymentOrderId, normalizedPhone, nonce);
    }

    private boolean isUuid(String value) {
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private HappyGalleryException invalidProof() {
        return new HappyGalleryException(ErrorCode.INVALID_INPUT, "결제 휴대폰 인증 정보가 올바르지 않습니다.");
    }

    private PhoneVerificationPurpose verificationPurpose(PaymentContext context) {
        return switch (context) {
            case ORDER -> PhoneVerificationPurpose.GUEST_ORDER;
            case BOOKING -> PhoneVerificationPurpose.GUEST_BOOKING;
            case PASS -> throw invalidProof();
        };
    }
}
