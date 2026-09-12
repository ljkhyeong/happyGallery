package com.personal.happygallery.application.security;

import com.personal.happygallery.application.security.port.in.BotProtectionUseCase;
import com.personal.happygallery.application.security.port.out.BotChallengeVerifier;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import org.springframework.stereotype.Service;

@Service
public class DefaultBotProtectionService implements BotProtectionUseCase {
    private final BotChallengeVerifier verifier;

    public DefaultBotProtectionService(BotChallengeVerifier verifier) {
        this.verifier = verifier;
    }

    @Override
    public String siteKey() {
        return verifier.siteKey();
    }

    @Override
    public void verify(String token, String action) {
        if (siteKey() == null) return;
        if (token == null || token.isBlank() || token.length() > 2048 || !verifier.verify(token, action)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "자동 입력 방지 확인 후 다시 시도해 주세요.");
        }
    }
}
