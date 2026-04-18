package com.personal.happygallery.application.customer.port.out;

public interface OAuthTokenExchangePort {

    record OAuthUserInfo(String providerId, String email, String name) {}

    record AuthorizationUrl(String url, String state) {}

    AuthorizationUrl buildAuthorizationUrl(String redirectUri, String state);

    OAuthUserInfo exchangeCodeForUserInfo(String authorizationCode, String redirectUri);
}
