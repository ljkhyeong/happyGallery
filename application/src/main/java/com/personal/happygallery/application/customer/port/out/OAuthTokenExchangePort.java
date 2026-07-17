package com.personal.happygallery.application.customer.port.out;

import com.personal.happygallery.domain.user.SocialProvider;

public interface OAuthTokenExchangePort {

    record OAuthUserInfo(String providerId, String email, String name) {}

    record AuthorizationUrl(String url, String state) {}

    SocialProvider provider();

    AuthorizationUrl buildAuthorizationUrl(String redirectUri, String state);

    OAuthUserInfo exchangeCodeForUserInfo(String authorizationCode, String redirectUri, String state);
}
