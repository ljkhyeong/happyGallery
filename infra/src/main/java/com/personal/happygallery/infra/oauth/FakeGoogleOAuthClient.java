package com.personal.happygallery.infra.oauth;

import com.personal.happygallery.app.customer.port.out.OAuthTokenExchangePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!prod")
class FakeGoogleOAuthClient implements OAuthTokenExchangePort {

    private static final Logger log = LoggerFactory.getLogger(FakeGoogleOAuthClient.class);

    @Override
    public AuthorizationUrl buildAuthorizationUrl(String redirectUri, String state) {
        log.info("[FAKE] Google OAuth authorization URL — redirectUri={}, state={}", redirectUri, state);
        return new AuthorizationUrl(
                "https://accounts.google.com/o/oauth2/v2/auth?fake=true&redirect_uri=" + redirectUri + "&state=" + state,
                state);
    }

    @Override
    public OAuthUserInfo exchangeCodeForUserInfo(String authorizationCode, String redirectUri) {
        log.info("[FAKE] Google OAuth code exchange — code={}, redirectUri={}", authorizationCode, redirectUri);
        return new OAuthUserInfo(
                "fake-google-sub-" + authorizationCode.hashCode(),
                "social-test@example.com",
                "테스트 구글 사용자"
        );
    }
}
