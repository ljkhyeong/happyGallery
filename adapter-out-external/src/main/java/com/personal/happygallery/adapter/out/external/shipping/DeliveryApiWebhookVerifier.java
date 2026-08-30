package com.personal.happygallery.adapter.out.external.shipping;

import com.personal.happygallery.application.order.port.in.ShipmentTrackingWebhookVerifier;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DeliveryApiWebhookVerifier implements ShipmentTrackingWebhookVerifier {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration TIMESTAMP_TOLERANCE = Duration.ofMinutes(5);

    private final DeliveryApiProperties properties;
    private final Clock clock;

    public DeliveryApiWebhookVerifier(DeliveryApiProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public boolean verify(String timestamp, String signature, byte[] body) {
        if (!StringUtils.hasText(properties.webhookSecret())
                || timestamp == null
                || signature == null
                || body == null) {
            return false;
        }
        Instant signedAt = parseTimestamp(timestamp);
        if (signedAt == null
                || Duration.between(signedAt, clock.instant()).abs().compareTo(TIMESTAMP_TOLERANCE) > 0) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.webhookSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
            mac.update((byte) '.');
            byte[] expected = mac.doFinal(body);
            String normalized = signature.startsWith("sha256=")
                    ? signature.substring("sha256=".length())
                    : signature;
            return MessageDigest.isEqual(expected, HexFormat.of().parseHex(normalized));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            return false;
        }
    }

    private Instant parseTimestamp(String timestamp) {
        try {
            return Instant.ofEpochSecond(Long.parseLong(timestamp));
        } catch (NumberFormatException ignored) {
            try {
                return Instant.parse(timestamp);
            } catch (DateTimeParseException invalidTimestamp) {
                return null;
            }
        }
    }
}
