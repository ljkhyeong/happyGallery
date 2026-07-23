package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class SocialOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    private final DefaultOAuth2AuthorizationRequestResolver delegate;
    private final SocialAccountLinkIntentStore linkIntentStore;

    public SocialOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrations,
                                                    SocialAccountLinkIntentStore linkIntentStore) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrations, CustomerSecurityRoutes.SOCIAL_AUTHORIZATION_BASE_URI);
        this.linkIntentStore = linkIntentStore;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return bindLinkState(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return bindLinkState(request, delegate.resolve(request, clientRegistrationId));
    }

    private OAuth2AuthorizationRequest bindLinkState(HttpServletRequest request,
                                                     OAuth2AuthorizationRequest authorizationRequest) {
        if (authorizationRequest == null) {
            return null;
        }

        String attemptId = request.getParameter(SocialAccountLinkIntentStore.LINK_ATTEMPT_PARAMETER);
        if (!StringUtils.hasText(attemptId)) {
            linkIntentStore.clear(request);
            return authorizationRequest;
        }

        try {
            String registrationId = authorizationRequest.getAttribute(OAuth2ParameterNames.REGISTRATION_ID);
            SocialProvider provider = SocialProvider.fromPath(registrationId);
            boolean bound = linkIntentStore.bindOauthState(
                    request, attemptId, provider, authorizationRequest.getState());
            return bound ? authorizationRequest : null;
        } catch (HappyGalleryException exception) {
            linkIntentStore.clear(request);
            return null;
        }
    }
}
