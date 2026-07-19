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

    public SocialLoginCommand resolve(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken token)) {
            throw socialLoginFailed();
        }

        SocialProvider provider = SocialProvider.fromPath(token.getAuthorizedClientRegistrationId());
        return switch (provider) {
            case GOOGLE -> googleProfile(provider, token);
            case NAVER -> naverProfile(provider, token.getPrincipal().getAttributes());
        };
    }

    private SocialLoginCommand googleProfile(SocialProvider provider, OAuth2AuthenticationToken token) {
        if (!(token.getPrincipal() instanceof OidcUser user)
                || !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw socialLoginFailed();
        }
        return new SocialLoginCommand(
                provider,
                required(user.getSubject()),
                required(user.getEmail()),
                required(user.getFullName()));
    }

    private SocialLoginCommand naverProfile(SocialProvider provider, Map<String, Object> attributes) {
        return new SocialLoginCommand(
                provider,
                required(attributes.get("id")),
                required(attributes.get("email")),
                required(attributes.get("name")));
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
}
