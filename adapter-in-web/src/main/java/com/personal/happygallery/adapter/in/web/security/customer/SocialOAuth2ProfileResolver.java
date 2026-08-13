package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginCommand;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SocialOAuth2ProfileResolver {

    public SocialIdentity resolveIdentity(Authentication authentication) {
        return resolveIdentity(requiredToken(authentication));
    }

    private SocialIdentity resolveIdentity(OAuth2AuthenticationToken token) {
        SocialProvider provider = provider(token);
        return switch (provider) {
            case GOOGLE -> new SocialIdentity(provider, required(googleUser(token).getSubject()));
            case NAVER -> new SocialIdentity(
                    provider, required(token.getPrincipal().getAttributes().get("id")));
        };
    }

    public SocialLoginCommand resolveLogin(Authentication authentication) {
        OAuth2AuthenticationToken token = requiredToken(authentication);
        return switch (provider(token)) {
            case GOOGLE -> googleProfile(token);
            case NAVER -> naverProfile(token.getPrincipal().getAttributes());
        };
    }

    private SocialLoginCommand googleProfile(OAuth2AuthenticationToken token) {
        OidcUser user = googleUser(token);
        if (!Boolean.TRUE.equals(user.getEmailVerified())) {
            throw socialLoginFailed();
        }
        return new SocialLoginCommand(
                SocialProvider.GOOGLE,
                required(user.getSubject()),
                required(user.getEmail()),
                required(user.getFullName()));
    }

    private SocialLoginCommand naverProfile(Map<String, Object> attributes) {
        return new SocialLoginCommand(
                SocialProvider.NAVER,
                required(attributes.get("id")),
                null,
                required(attributes.get("name")));
    }

    private SocialProvider provider(OAuth2AuthenticationToken token) {
        return SocialProvider.fromPath(token.getAuthorizedClientRegistrationId());
    }

    private OAuth2AuthenticationToken requiredToken(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            throw socialLoginFailed();
        }
        return token;
    }

    private OidcUser googleUser(OAuth2AuthenticationToken token) {
        if (!(token.getPrincipal() instanceof OidcUser user)) {
            throw socialLoginFailed();
        }
        return user;
    }

    private String required(Object value) {
        if (!(value instanceof String text) || !StringUtils.hasText(text)) {
            throw socialLoginFailed();
        }
        return text;
    }

    private HappyGalleryException socialLoginFailed() {
        return new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    public record SocialIdentity(SocialProvider provider, String providerId) {}
}
