package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.user.User;

public interface SocialAuthUseCase {

    record SocialLoginCommand(String authorizationCode, String redirectUri) {}

    record SocialLoginResult(User user, boolean newUser) {}

    record AuthorizationUrlResult(String url, String state) {}

    AuthorizationUrlResult buildAuthorizationUrl(String redirectUri);

    SocialLoginResult socialLogin(SocialLoginCommand command);
}
