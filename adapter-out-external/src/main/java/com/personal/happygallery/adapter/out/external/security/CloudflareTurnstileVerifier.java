package com.personal.happygallery.adapter.out.external.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.personal.happygallery.application.security.port.out.BotChallengeVerifier;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class CloudflareTurnstileVerifier implements BotChallengeVerifier {
    private final TurnstileProperties properties;
    private final RestClient client;

    CloudflareTurnstileVerifier(TurnstileProperties properties,
            @Qualifier("turnstileRestClient") RestClient client) {
        this.properties = properties;
        this.client = client;
    }

    @Override
    public String siteKey() {
        return properties.enabled() ? properties.siteKey() : null;
    }

    @Override
    public boolean verify(String token, String action) {
        try {
            VerificationResponse result = client.post().uri("/turnstile/v0/siteverify")
                    .body(new VerificationRequest(properties.secretKey(), token))
                    .retrieve().body(VerificationResponse.class);
            if (result == null || result.success() == null || result.configurationError()) {
                throw unavailable();
            }
            return result.success() && properties.hostname().equals(result.hostname())
                    && action.equals(result.action());
        } catch (RestClientException exception) {
            // 비밀 키·토큰과 외부 응답 본문을 오류에 포함하지 않는다.
            throw unavailable();
        }
    }

    private static HappyGalleryException unavailable() {
        return new HappyGalleryException(ErrorCode.SERVICE_UNAVAILABLE,
                "자동 입력 방지를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.");
    }

    private record VerificationRequest(String secret, String response) {}

    private record VerificationResponse(Boolean success, String hostname, String action,
            @JsonProperty("error-codes") List<String> errorCodes) {
        boolean configurationError() {
            return errorCodes != null && errorCodes.stream().anyMatch(code ->
                    List.of("missing-input-secret", "invalid-input-secret", "bad-request", "internal-error").contains(code));
        }
    }
}
