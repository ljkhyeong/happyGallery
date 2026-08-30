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
            case NAVER, KAKAO -> new SocialIdentity(
                    provider, requiredIdentifier(token.getPrincipal().getAttributes().get("id")));
        };
    }

    public SocialLoginCommand resolveLogin(Authentication authentication) {
        OAuth2AuthenticationToken token = requiredToken(authentication);
        return switch (provider(token)) {
            case GOOGLE -> googleProfile(token);
            case NAVER -> naverProfile(token.getPrincipal().getAttributes());
            case KAKAO -> kakaoProfile(token.getPrincipal().getAttributes());
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
                requiredIdentifier(attributes.get("id")),
                null,
                required(attributes.get("name")));
    }

    private SocialLoginCommand kakaoProfile(Map<String, Object> attributes) {
        if (!Boolean.TRUE.equals(attributes.get("is_email_valid"))
                || !Boolean.TRUE.equals(attributes.get("is_email_verified"))) {
            throw socialLoginFailed();
        }
        return new SocialLoginCommand(
                SocialProvider.KAKAO,
                requiredIdentifier(attributes.get("id")),
                required(attributes.get("email")),
                required(attributes.get("nickname")));
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

    private String requiredIdentifier(Object value) {
        if (value instanceof Number number) {
            return number.toString();
        }
        return required(value);
    }

    private HappyGalleryException socialLoginFailed() {
        return new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
    }

    public record SocialIdentity(SocialProvider provider, String providerId) {}
}
