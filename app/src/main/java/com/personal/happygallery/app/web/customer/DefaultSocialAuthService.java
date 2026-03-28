package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.app.customer.port.out.OAuthTokenExchangePort;
import com.personal.happygallery.app.customer.port.out.OAuthTokenExchangePort.OAuthUserInfo;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.customer.port.out.UserStorePort;
import com.personal.happygallery.common.crypto.BlindIndexer;
import com.personal.happygallery.common.crypto.FieldEncryptor;
import com.personal.happygallery.domain.user.AuthProvider;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultSocialAuthService implements SocialAuthUseCase {

    private final OAuthTokenExchangePort oauthPort;
    private final UserReaderPort userReader;
    private final UserStorePort userStore;
    private final FieldEncryptor fieldEncryptor;
    private final BlindIndexer blindIndexer;
    private final Clock clock;

    public DefaultSocialAuthService(OAuthTokenExchangePort oauthPort,
                                    UserReaderPort userReader,
                                    UserStorePort userStore,
                                    FieldEncryptor fieldEncryptor,
                                    BlindIndexer blindIndexer,
                                    Clock clock) {
        this.oauthPort = oauthPort;
        this.userReader = userReader;
        this.userStore = userStore;
        this.fieldEncryptor = fieldEncryptor;
        this.blindIndexer = blindIndexer;
        this.clock = clock;
    }

    @Override
    @Transactional
    public SocialLoginResult socialLogin(SocialLoginCommand command) {
        OAuthUserInfo info = oauthPort.exchangeCodeForUserInfo(
                command.authorizationCode(), command.redirectUri());

        // 1. provider + providerId로 기존 유저 조회
        Optional<User> byProvider = userReader.findByProviderAndProviderId(
                AuthProvider.GOOGLE, info.providerId());
        if (byProvider.isPresent()) {
            User user = byProvider.get();
            user.updateLastLoginAt(LocalDateTime.now(clock));
            return new SocialLoginResult(user, false);
        }

        // 2. 이메일로 기존 LOCAL 유저 조회 → 계정 연결
        Optional<User> byEmail = userReader.findByEmail(info.email());
        if (byEmail.isPresent()) {
            User user = byEmail.get();
            user.linkProvider(AuthProvider.GOOGLE, info.providerId());
            user.updateLastLoginAt(LocalDateTime.now(clock));
            return new SocialLoginResult(user, false);
        }

        // 3. 신규 유저 생성
        User newUser = new User(info.email(), info.name(), AuthProvider.GOOGLE, info.providerId());
        newUser.applyEncryption(
                fieldEncryptor.encrypt(info.email()), blindIndexer.index(info.email()),
                null, null);
        userStore.save(newUser);
        return new SocialLoginResult(newUser, true);
    }
}
