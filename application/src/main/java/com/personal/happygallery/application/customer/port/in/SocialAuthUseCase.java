package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.List;

public interface SocialAuthUseCase {

    record SocialLoginCommand(SocialProvider provider, String providerId, String email, String name) {}

    record SocialLoginResult(User user, boolean newUser) {}

    record SocialLinkCommand(Long userId,
                             long credentialVersion,
                             SocialProvider provider,
                             String providerId) {}

    SocialLoginResult socialLogin(SocialLoginCommand command);

    List<SocialProvider> listLinkedProviders(Long userId);

    void linkSocialAccount(SocialLinkCommand command);

    boolean unlinkSocialAccount(Long userId, SocialProvider provider);
}
