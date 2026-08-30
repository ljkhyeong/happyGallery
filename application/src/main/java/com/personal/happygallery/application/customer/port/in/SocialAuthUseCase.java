package com.personal.happygallery.application.customer.port.in;

import com.personal.happygallery.application.policy.PolicyAcceptance;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.List;

public interface SocialAuthUseCase {

    record SocialLoginCommand(SocialProvider provider,
                              String providerId,
                              String verifiedEmail,
                              String name,
                              PolicyAcceptance policyAcceptance) {

        public SocialLoginCommand(
                SocialProvider provider, String providerId, String verifiedEmail, String name) {
            this(provider, providerId, verifiedEmail, name, null);
        }

        public SocialLoginCommand withPolicyAcceptance(PolicyAcceptance acceptance) {
            return new SocialLoginCommand(provider, providerId, verifiedEmail, name, acceptance);
        }
    }

    record SocialLoginResult(User user, boolean newUser) {}

    record SocialLinkCommand(Long userId,
                             long credentialVersion,
                             SocialProvider provider,
                             String providerId,
                             boolean recentlyReauthenticated) {}

    record SocialReauthenticationCommand(Long userId,
                                         long credentialVersion,
                                         SocialProvider provider,
                                         String providerId) {}

    record SocialUnlinkCommand(Long userId,
                               long credentialVersion,
                               SocialProvider provider,
                               boolean recentlyReauthenticated) {}

    SocialLoginResult socialLogin(SocialLoginCommand command);

    List<SocialProvider> listLinkedProviders(Long userId);

    void linkSocialAccount(SocialLinkCommand command);

    void verifyLinkedSocialAccount(SocialReauthenticationCommand command);

    boolean unlinkSocialAccount(SocialUnlinkCommand command);
}
