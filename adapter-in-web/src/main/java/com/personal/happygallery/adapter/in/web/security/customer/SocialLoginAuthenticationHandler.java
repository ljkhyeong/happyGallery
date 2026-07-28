package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.adapter.in.web.customer.CustomerSessionBinder;
import com.personal.happygallery.adapter.in.web.security.customer.SocialOAuth2ProfileResolver.SocialIdentity;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLinkCommand;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SocialLoginAuthenticationHandler
        implements AuthenticationSuccessHandler, AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(SocialLoginAuthenticationHandler.class);
    private static final String FRONTEND_CALLBACK_PATH = "/auth/callback";

    private final SocialAuthUseCase socialAuth;
    private final SocialOAuth2ProfileResolver profileResolver;
    private final SocialAccountLinkIntentStore linkIntentStore;
    private final SocialSignupIntentStore signupIntentStore;
    private final CustomerSessionBinder customerSessionBinder;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public SocialLoginAuthenticationHandler(SocialAuthUseCase socialAuth,
                                            SocialOAuth2ProfileResolver profileResolver,
                                            SocialAccountLinkIntentStore linkIntentStore,
                                            SocialSignupIntentStore signupIntentStore,
                                            CustomerSessionBinder customerSessionBinder) {
        this.socialAuth = socialAuth;
        this.profileResolver = profileResolver;
        this.linkIntentStore = linkIntentStore;
        this.signupIntentStore = signupIntentStore;
        this.customerSessionBinder = customerSessionBinder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            SocialIdentity identity = profileResolver.resolveIdentity(authentication);
            var linkIntent = linkIntentStore.consume(request, identity.provider());
            if (linkIntent.isPresent()) {
                signupIntentStore.clear(request);
                socialAuth.linkSocialAccount(new SocialLinkCommand(
                        linkIntent.get().userId(),
                        linkIntent.get().credentialVersion(),
                        identity.provider(),
                        identity.providerId()));
                redirect(request, response, "linked", identity.provider().name());
                return;
            }

            SocialAuthUseCase.SocialLoginCommand profile = profileResolver.resolveLogin(authentication)
                    .withPolicyAcceptance(signupIntentStore.consume(request, identity.provider()).orElse(null));
            SocialAuthUseCase.SocialLoginResult result = socialAuth.socialLogin(profile);
            customerSessionBinder.bind(request, response, result.user());
            redirect(request, response, "newUser", String.valueOf(result.newUser()));
        } catch (HappyGalleryException exception) {
            ErrorCode errorCode = socialErrorCode(exception.getErrorCode());
            redirect(request, response, "error", errorCode.name());
        } catch (RuntimeException exception) {
            log.error("Social login completion failed", exception);
            redirect(request, response, "error", ErrorCode.SOCIAL_LOGIN_FAILED.name());
        } finally {
            linkIntentStore.clear(request);
            signupIntentStore.clear(request);
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        linkIntentStore.clear(request);
        signupIntentStore.clear(request);
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            log.warn("Social OAuth authentication failed: {}", oauth2Exception.getError().getErrorCode());
        } else {
            log.warn("Social OAuth authentication failed: {}", exception.getClass().getSimpleName());
        }
        SecurityContextHolder.clearContext();
        redirect(request, response, "error", ErrorCode.SOCIAL_LOGIN_FAILED.name());
    }

    private ErrorCode socialErrorCode(ErrorCode errorCode) {
        return switch (errorCode) {
            case SOCIAL_ACCOUNT_LINK_REQUIRED,
                 SOCIAL_ACCOUNT_ALREADY_LINKED,
                 SOCIAL_PROVIDER_ALREADY_LINKED,
                 LAST_LOGIN_METHOD_REQUIRED,
                 POLICY_CONSENT_REQUIRED -> errorCode;
            default -> ErrorCode.SOCIAL_LOGIN_FAILED;
        };
    }

    private void redirect(HttpServletRequest request,
                          HttpServletResponse response,
                          String parameter,
                          String value) throws IOException {
        String target = UriComponentsBuilder.fromPath(FRONTEND_CALLBACK_PATH)
                .queryParam(parameter, value)
                .build()
                .encode()
                .toUriString();
        redirectStrategy.sendRedirect(request, response, target);
    }
}
