package com.personal.happygallery.application.payment;

import com.personal.happygallery.application.customer.PhoneVerificationConsumptionService;
import com.personal.happygallery.application.token.GuestTokenProperties;
import com.personal.happygallery.application.token.TokenSigningException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/** 비회원 결제 준비에서 소비한 휴대폰 인증을 결제 시도에 귀속시키는 서명 증거를 관리한다. */
@Component
public class GuestPaymentVerificationService {

    private static final String VERSION = "v1";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final PhoneVerificationConsumptionService phoneVerificationConsumption;
    private final GuestTokenProperties tokenProperties;

    public GuestPaymentVerificationService(
            PhoneVerificationConsumptionService phoneVerificationConsumption,
            GuestTokenProperties tokenProperties) {
        this.phoneVerificationConsumption = phoneVerificationConsumption;
        this.tokenProperties = tokenProperties;
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
        String signature = sign(canonical(context, paymentOrderId, normalizedPhone, nonce),
                tokenProperties.hmacSecret());
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
        boolean valid = matches(parts[2], sign(canonical, tokenProperties.hmacSecret()));
        if (!tokenProperties.previousHmacSecret().isBlank()) {
            valid |= matches(parts[2], sign(canonical, tokenProperties.previousHmacSecret()));
        }
        if (!valid) {
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

    private boolean matches(String actual, String expected) {
        return MessageDigest.isEqual(
                Objects.requireNonNullElse(actual, "").getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private String sign(String value, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new TokenSigningException();
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
