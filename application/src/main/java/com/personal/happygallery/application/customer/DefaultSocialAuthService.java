package com.personal.happygallery.application.customer;

import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.out.SocialAccountReaderPort;
import com.personal.happygallery.application.customer.port.out.SocialAccountStorePort;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.EmailAddress;
import com.personal.happygallery.domain.user.SocialAccount;
import com.personal.happygallery.domain.user.SocialProvider;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultSocialAuthService implements SocialAuthUseCase {

    private final SocialAccountReaderPort socialAccountReader;
    private final SocialAccountStorePort socialAccountStore;
    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public DefaultSocialAuthService(SocialAccountReaderPort socialAccountReader,
                                    SocialAccountStorePort socialAccountStore,
                                    UserReaderPort userReader,
                                    UserStorePort userStore,
                                    ApplicationEventPublisher eventPublisher,
                                    Clock clock) {
        this.socialAccountReader = socialAccountReader;
        this.socialAccountStore = socialAccountStore;
        this.userReader = userReader;
        this.userStore = userStore;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SocialLoginResult socialLogin(SocialLoginCommand command) {
        Optional<SocialAccount> socialAccount = socialAccountReader.findByProviderAndProviderId(
                command.provider(), command.providerId());
        if (socialAccount.isPresent()) {
            User user = findSocialAccountUserForUpdate(socialAccount.get());
            boolean stillLinked = socialAccountReader.findByProviderAndProviderId(
                            command.provider(), command.providerId())
                    .map(SocialAccount::getUserId)
                    .filter(user.getId()::equals)
                    .isPresent();
            if (!stillLinked) {
                throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
            }
            updateLastLogin(user);
            return new SocialLoginResult(user, false);
        }

        String canonicalEmail = switch (command.provider()) {
            case GOOGLE -> EmailAddress.required(command.verifiedEmail());
            case NAVER -> null;
        };
        if (canonicalEmail != null && userReader.findByEmail(canonicalEmail).isPresent()) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
        }

        User user;
        try {
            user = userStore.save(User.fromSocialProfile(canonicalEmail, command.name()));
        } catch (HappyGalleryException exception) {
            if (exception.getErrorCode() == ErrorCode.EMAIL_ALREADY_EXISTS) {
                throw new HappyGalleryException(ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED);
            }
            throw exception;
        }
        socialAccountStore.save(new SocialAccount(user.getId(), command.provider(), command.providerId()));
        updateLastLogin(user);

        return new SocialLoginResult(user, true);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SocialProvider> listLinkedProviders(Long userId) {
        return socialAccountReader.findByUserId(userId).stream()
                .map(SocialAccount::getProvider)
                .toList();
    }

    @Override
    @Transactional
    public void linkSocialAccount(SocialLinkCommand command) {
        User user = findUserForUpdate(command.userId());
        if (user.getCredentialVersion() != command.credentialVersion()) {
            throw new HappyGalleryException(ErrorCode.UNAUTHORIZED);
        }

        Optional<SocialAccount> linkedIdentity = socialAccountReader.findByProviderAndProviderId(
                command.provider(), command.providerId());
        if (linkedIdentity.isPresent()) {
            if (command.userId().equals(linkedIdentity.get().getUserId())) {
                return;
            }
            throw new HappyGalleryException(ErrorCode.SOCIAL_ACCOUNT_ALREADY_LINKED);
        }
        if (socialAccountReader.findByUserIdAndProvider(command.userId(), command.provider()).isPresent()) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_PROVIDER_ALREADY_LINKED);
        }

        socialAccountStore.save(new SocialAccount(
                command.userId(), command.provider(), command.providerId()));
    }

    @Override
    @Transactional
    public boolean unlinkSocialAccount(Long userId, SocialProvider provider) {
        User user = findUserForUpdate(userId);
        List<SocialAccount> linkedAccounts = socialAccountReader.findByUserId(userId);
        boolean linked = linkedAccounts.stream()
                .anyMatch(account -> account.getProvider() == provider);
        if (!linked) {
            return false;
        }
        if (!user.hasLocalPassword() && linkedAccounts.size() == 1) {
            throw new HappyGalleryException(ErrorCode.LAST_LOGIN_METHOD_REQUIRED);
        }
        long invalidatedCredentialVersion = user.getCredentialVersion();
        socialAccountStore.deleteByUserIdAndProvider(userId, provider);
        user.markAuthenticationMethodsChanged();
        userStore.save(user);
        eventPublisher.publishEvent(new CustomerCredentialsChangedEvent(
                userId, invalidatedCredentialVersion));
        return true;
    }

    private User findSocialAccountUserForUpdate(SocialAccount socialAccount) {
        return userReader.findByIdForUpdate(socialAccount.getUserId())
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED));
    }

    private User findUserForUpdate(Long userId) {
        return userReader.findByIdForUpdate(userId)
                .orElseThrow(() -> new HappyGalleryException(ErrorCode.UNAUTHORIZED));
    }

    private void updateLastLogin(User user) {
        user.updateLastLoginAt(LocalDateTime.now(clock));
    }
}
