package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
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
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultSocialAuthService implements SocialAuthUseCase {

    private final SocialAccountReaderPort socialAccountReader;
    private final SocialAccountStorePort socialAccountStore;
    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final Clock clock;

    public DefaultSocialAuthService(SocialAccountReaderPort socialAccountReader,
                                    SocialAccountStorePort socialAccountStore,
                                    UserReaderPort userReader,
                                    UserStorePort userStore,
                                    Clock clock) {
        this.socialAccountReader = socialAccountReader;
        this.socialAccountStore = socialAccountStore;
        this.userReader = userReader;
        this.userStore = userStore;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SocialLoginResult socialLogin(SocialLoginCommand command) {
        Optional<SocialAccount> socialAccount = socialAccountReader.findByProviderAndProviderId(
                command.provider(), command.providerId());
        if (socialAccount.isPresent()) {
            User user = findSocialAccountUser(socialAccount.get());
            updateLastLogin(user);
            return new SocialLoginResult(user, false);
        }

        Optional<User> existingUser = userReader.findByEmail(command.email());
        if (existingUser.isPresent()) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
        }

        User user = userStore.save(User.fromSocialProfile(command.email(), command.name()));
        socialAccountStore.save(new SocialAccount(user.getId(), command.provider(), command.providerId()));
        updateLastLogin(user);

        return new SocialLoginResult(user, true);
    }

    private User findSocialAccountUser(SocialAccount socialAccount) {
        return userReader.findById(socialAccount.getUserId())
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED));
    }

    private void updateLastLogin(User user) {
        user.updateLastLoginAt(LocalDateTime.now(clock));
    }
}
