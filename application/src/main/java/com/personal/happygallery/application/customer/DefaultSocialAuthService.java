package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.out.OAuthTokenExchangePort;
import com.personal.happygallery.application.customer.port.out.OAuthTokenExchangePort.AuthorizationUrl;
import com.personal.happygallery.application.customer.port.out.OAuthTokenExchangePort.OAuthUserInfo;
import com.personal.happygallery.application.customer.port.out.SocialAccountReaderPort;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultSocialAuthService implements SocialAuthUseCase {

    private final Map<SocialProvider, OAuthTokenExchangePort> oauthPorts;
    private final SocialAccountReaderPort socialAccountReader;
    private final SocialAccountStorePort socialAccountStore;
    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final Clock clock;

    public DefaultSocialAuthService(List<OAuthTokenExchangePort> oauthPorts,
                                    SocialAccountReaderPort socialAccountReader,
                                    SocialAccountStorePort socialAccountStore,
                                    UserReaderPort userReader,
                                    UserStorePort userStore,
                                    Clock clock) {
        this.oauthPorts = new EnumMap<>(SocialProvider.class);
        oauthPorts.forEach(oauthPort -> this.oauthPorts.put(oauthPort.provider(), oauthPort));
        this.socialAccountReader = socialAccountReader;
        this.socialAccountStore = socialAccountStore;
        this.userReader = userReader;
        this.userStore = userStore;
        this.clock = clock;
    }

    @Override
    public AuthorizationUrlResult buildAuthorizationUrl(SocialProvider provider, String redirectUri) {
        String state = UUID.randomUUID().toString();
        AuthorizationUrl authUrl = oauthPort(provider).buildAuthorizationUrl(redirectUri, state);
        return new AuthorizationUrlResult(authUrl.url(), authUrl.state());
    }

    @Override
    @Transactional
    public SocialLoginResult socialLogin(SocialLoginCommand command) {
        OAuthUserInfo info = oauthPort(command.provider()).exchangeCodeForUserInfo(
                command.authorizationCode(), command.redirectUri(), command.state());

        Optional<SocialAccount> socialAccount = socialAccountReader.findByProviderAndProviderId(
                command.provider(), info.providerId());
        if (socialAccount.isPresent()) {
            User user = findSocialAccountUser(socialAccount.get());
            updateLastLogin(user);
            return new SocialLoginResult(user, false);
        }

        Optional<User> existingUser = userReader.findByEmail(info.email());
        if (existingUser.isPresent()) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
        }

        User user = userStore.save(User.fromSocialProfile(info.email(), info.name()));
        socialAccountStore.save(new SocialAccount(user.getId(), command.provider(), info.providerId()));
        updateLastLogin(user);

        return new SocialLoginResult(user, true);
    }

    private OAuthTokenExchangePort oauthPort(SocialProvider provider) {
        OAuthTokenExchangePort oauthPort = oauthPorts.get(provider);
        if (oauthPort == null) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return oauthPort;
    }

    private User findSocialAccountUser(SocialAccount socialAccount) {
        return userReader.findById(socialAccount.getUserId())
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED));
    }

    private void updateLastLogin(User user) {
        user.updateLastLoginAt(LocalDateTime.now(clock));
    }
}
