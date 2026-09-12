package com.personal.happygallery.adapter.out.external.shipping;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeliveryApiWebhookVerifierTest {

    private static final String SECRET = "webhook-test-secret";
    private static final Instant NOW = Instant.parse("2026-08-27T06:00:00Z");

    @Test
    @DisplayName("현재 시각의 원문 본문 서명이 일치하면 웹훅을 허용한다")
    void verify_acceptsValidSignature() throws Exception {
        byte[] body = "{\"event\":\"tracking.updated\"}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());
        DeliveryApiWebhookVerifier verifier = verifier();

        assertThat(verifier.verify(timestamp, sign(timestamp, body), body)).isTrue();
    }

    @Test
    @DisplayName("허용 시각을 지난 웹훅은 서명이 맞아도 거절한다")
    void verify_rejectsStaleTimestamp() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.minus(Duration.ofMinutes(6)).getEpochSecond());

        assertThat(verifier().verify(timestamp, sign(timestamp, body), body)).isFalse();
    }

    private DeliveryApiWebhookVerifier verifier() {
        return verifier(true);
    }

    @Test
    @DisplayName("연동을 끄면 비밀키가 남아 있어도 웹훅을 거절한다")
    void verify_rejectsDisabledIntegration() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());
        assertThat(verifier(false).verify(timestamp, sign(timestamp, body), body)).isFalse();
    }

    @Test
    @DisplayName("본문 변조와 표현 범위를 벗어난 시각을 거절한다")
    void verify_rejectsTamperedBodyAndOutOfRangeTime() throws Exception {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
        String timestamp = Long.toString(NOW.getEpochSecond());
        assertThat(verifier().verify(timestamp, sign(timestamp, body), "[]".getBytes(StandardCharsets.UTF_8))).isFalse();
        assertThat(verifier().verify(Long.toString(Long.MAX_VALUE), "signature", body)).isFalse();
    }

    private DeliveryApiWebhookVerifier verifier(boolean enabled) {
        return new DeliveryApiWebhookVerifier(
                new DeliveryApiProperties(
                        enabled,
                        "api-key",
                        "secret-key",
                        "endpoint-id",
                        SECRET,
                        "https://api.deliveryapi.co.kr",
                        Duration.ofSeconds(3),
                        Duration.ofSeconds(1),
                        Duration.ofMillis(500),
                        10,
                        Duration.ofSeconds(30)),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private static String sign(String timestamp, byte[] body) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update(timestamp.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) '.');
        return HexFormat.of().formatHex(mac.doFinal(body));
    }
}
