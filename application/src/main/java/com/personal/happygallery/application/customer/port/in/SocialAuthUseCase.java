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
                              PolicyAcceptance policyAcceptance,
                              String providerPhone) {

        public SocialLoginCommand(
                SocialProvider provider, String providerId, String verifiedEmail, String name) {
            this(provider, providerId, verifiedEmail, name, null, null);
        }

        public SocialLoginCommand(SocialProvider provider, String providerId, String verifiedEmail,
                                  String name, PolicyAcceptance policyAcceptance) {
            this(provider, providerId, verifiedEmail, name, policyAcceptance, null);
        }

        public SocialLoginCommand withProviderPhone(String phone) {
            return new SocialLoginCommand(provider, providerId, verifiedEmail, name, policyAcceptance, phone);
        }

        public SocialLoginCommand withPolicyAcceptance(PolicyAcceptance acceptance) {
            return new SocialLoginCommand(provider, providerId, verifiedEmail, name, acceptance, providerPhone);
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
