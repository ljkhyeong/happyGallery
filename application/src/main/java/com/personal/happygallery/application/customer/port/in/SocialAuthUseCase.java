package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.SocialProvider;

public interface SocialAuthUseCase {

    record SocialLoginCommand(SocialProvider provider, String authorizationCode,
                              String redirectUri, String state) {}

    record SocialLoginResult(User user, boolean newUser) {}

    record AuthorizationUrlResult(String url, String state) {}

    AuthorizationUrlResult buildAuthorizationUrl(SocialProvider provider, String redirectUri);

    SocialLoginResult socialLogin(SocialLoginCommand command);
}
