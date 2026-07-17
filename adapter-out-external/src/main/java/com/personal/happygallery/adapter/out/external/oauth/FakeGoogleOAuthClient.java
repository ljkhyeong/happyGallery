package com.personal.happygallery.adapter.out.external.oauth;

import com.personal.happygallery.application.customer.port.out.OAuthTokenExchangePort;
import com.personal.happygallery.domain.user.SocialProvider;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("!prod")
class FakeGoogleOAuthClient implements OAuthTokenExchangePort {

    @Override
    public SocialProvider provider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public AuthorizationUrl buildAuthorizationUrl(String redirectUri, String state) {
        String callbackUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("code", "fake-google-code")
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return new AuthorizationUrl(callbackUrl, state);
    }

    @Override
    public OAuthUserInfo exchangeCodeForUserInfo(String authorizationCode, String redirectUri, String state) {
        return new OAuthUserInfo(
                "fake-google-sub-" + authorizationCode.hashCode(),
                "social-test@example.com",
                "테스트 구글 사용자"
        );
    }
}
