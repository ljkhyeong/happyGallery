package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.application.customer.port.out.OAuthTokenExchangePort;
import com.personal.happygallery.domain.user.SocialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("!prod")
class FakeNaverOAuthClient implements OAuthTokenExchangePort {

    private static final Logger log = LoggerFactory.getLogger(FakeNaverOAuthClient.class);

    @Override
    public SocialProvider provider() {
        return SocialProvider.NAVER;
    }

    @Override
    public AuthorizationUrl buildAuthorizationUrl(String redirectUri, String state) {
        log.info("[FAKE] Naver OAuth authorization URL — redirectUri={}", redirectUri);
        String callbackUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", "fake-naver-code")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return new AuthorizationUrl(callbackUrl, state);
    }

    @Override
    public OAuthUserInfo exchangeCodeForUserInfo(String authorizationCode, String redirectUri, String state) {
        log.info("[FAKE] Naver OAuth code exchange — redirectUri={}", redirectUri);
        return new OAuthUserInfo(
                "fake-naver-id-" + authorizationCode.hashCode(),
                "social-test@example.com",
                "테스트 네이버 사용자"
        );
    }
}
