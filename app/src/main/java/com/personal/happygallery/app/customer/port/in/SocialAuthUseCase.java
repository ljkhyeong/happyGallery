package com.personal.happygallery.app.customer.port.in;

import com.personal.happygallery.domain.user.User;

public interface SocialAuthUseCase {

    record SocialLoginCommand(String authorizationCode, String redirectUri) {}

    record SocialLoginResult(User user, boolean newUser) {}

    SocialLoginResult socialLogin(SocialLoginCommand command);
}
