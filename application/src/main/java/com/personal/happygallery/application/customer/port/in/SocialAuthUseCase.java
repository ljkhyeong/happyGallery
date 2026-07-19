package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.SocialProvider;

public interface SocialAuthUseCase {

    record SocialLoginCommand(SocialProvider provider, String providerId, String email, String name) {}

    record SocialLoginResult(User user, boolean newUser) {}

    SocialLoginResult socialLogin(SocialLoginCommand command);
}
