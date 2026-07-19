package com.personal.happygallery.adapter.in.web.security.customer;

import com.personal.happygallery.adapter.in.web.customer.CustomerSessionBinder;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
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
    private final CustomerSessionBinder customerSessionBinder;
    private final RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    public SocialLoginAuthenticationHandler(SocialAuthUseCase socialAuth,
                                            SocialOAuth2ProfileResolver profileResolver,
                                            CustomerSessionBinder customerSessionBinder) {
        this.socialAuth = socialAuth;
        this.profileResolver = profileResolver;
        this.customerSessionBinder = customerSessionBinder;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        try {
            SocialAuthUseCase.SocialLoginResult result = socialAuth.socialLogin(
                    profileResolver.resolve(authentication));
            customerSessionBinder.bind(request, response, result.user().getId());
            redirect(request, response, "newUser", String.valueOf(result.newUser()));
        } catch (HappyGalleryException exception) {
            ErrorCode errorCode = exception.getErrorCode() == ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED
                    ? ErrorCode.SOCIAL_ACCOUNT_LINK_REQUIRED
                    : ErrorCode.SOCIAL_LOGIN_FAILED;
            redirect(request, response, "error", errorCode.name());
        } catch (RuntimeException exception) {
            log.error("Social login completion failed", exception);
            redirect(request, response, "error", ErrorCode.SOCIAL_LOGIN_FAILED.name());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        if (exception instanceof OAuth2AuthenticationException oauth2Exception) {
            log.warn("Social OAuth authentication failed: {}", oauth2Exception.getError().getErrorCode());
        } else {
            log.warn("Social OAuth authentication failed: {}", exception.getClass().getSimpleName());
        }
        SecurityContextHolder.clearContext();
        redirect(request, response, "error", ErrorCode.SOCIAL_LOGIN_FAILED.name());
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
