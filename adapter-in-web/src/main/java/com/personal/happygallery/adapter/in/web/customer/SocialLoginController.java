package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialAuthUrlResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialLoginRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialLoginResponse;
import com.personal.happygallery.adapter.in.web.config.properties.SocialLoginProperties;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.AuthorizationUrlResult;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase.SocialLoginResult;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.user.SocialProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/social")
public class SocialLoginController {

    private static final String OAUTH_STATE_SESSION_ATTRIBUTE_PREFIX =
            SocialLoginController.class.getName() + ".state.";
    private static final String OAUTH_REDIRECT_URI_SESSION_ATTRIBUTE_PREFIX =
            SocialLoginController.class.getName() + ".redirect-uri.";

    private final SocialAuthUseCase socialAuth;
    private final CustomerSessionBinder customerSessionBinder;
    private final SocialLoginProperties properties;

    public SocialLoginController(SocialAuthUseCase socialAuth,
                                 CustomerSessionBinder customerSessionBinder,
                                 SocialLoginProperties properties) {
        this.socialAuth = socialAuth;
        this.customerSessionBinder = customerSessionBinder;
        this.properties = properties;
    }

    @GetMapping("/{provider}/url")
    public ResponseEntity<SocialAuthUrlResponse> authorizationUrl(@PathVariable String provider,
                                                                  @RequestParam String redirectUri,
                                                                  HttpServletRequest httpRequest) {
        SocialProvider socialProvider = SocialProvider.fromPath(provider);
        requireAllowedRedirectUri(socialProvider, redirectUri);
        AuthorizationUrlResult result = socialAuth.buildAuthorizationUrl(
                socialProvider, redirectUri);
        storeAuthorization(httpRequest, socialProvider, result.state(), redirectUri);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new SocialAuthUrlResponse(result.url(), result.state()));
    }

    @PostMapping("/{provider}")
    public SocialLoginResponse login(@PathVariable String provider,
                                     @RequestBody @Valid SocialLoginRequest request,
                                     HttpServletRequest httpRequest,
                                     HttpServletResponse httpResponse) {
        SocialProvider socialProvider = SocialProvider.fromPath(provider);
        VerifiedAuthorization authorization = verifyAndConsumeAuthorization(
                httpRequest, socialProvider, request.state(), request.redirectUri());
        SocialLoginResult result = socialAuth.socialLogin(
                new SocialAuthUseCase.SocialLoginCommand(
                        socialProvider,
                        request.code(),
                        authorization.redirectUri(),
                        authorization.state()));
        customerSessionBinder.bind(httpRequest, httpResponse, result.user().getId());
        return new SocialLoginResponse(
                CustomerUserResponse.from(result.user()),
                result.newUser());
    }

    private void requireAllowedRedirectUri(SocialProvider provider, String redirectUri) {
        if (!properties.redirectUri(provider).equals(redirectUri)) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "허용되지 않은 소셜 로그인 redirectUri입니다.");
        }
    }

    private void storeAuthorization(HttpServletRequest request,
                                    SocialProvider provider,
                                    String state,
                                    String redirectUri) {
        HttpSession session = request.getSession();
        session.setAttribute(stateAttributeName(provider), state);
        session.setAttribute(redirectUriAttributeName(provider), redirectUri);
    }

    private VerifiedAuthorization verifyAndConsumeAuthorization(HttpServletRequest request,
                                                                 SocialProvider provider,
                                                                 String actualState,
                                                                 String actualRedirectUri) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        String stateAttribute = stateAttributeName(provider);
        String redirectUriAttribute = redirectUriAttributeName(provider);
        Object expectedState = session.getAttribute(stateAttribute);
        Object expectedRedirectUri = session.getAttribute(redirectUriAttribute);
        session.removeAttribute(stateAttribute);
        session.removeAttribute(redirectUriAttribute);

        if (!(expectedState instanceof String state)
                || !(expectedRedirectUri instanceof String redirectUri)
                || !state.equals(actualState)
                || !redirectUri.equals(actualRedirectUri)) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return new VerifiedAuthorization(state, redirectUri);
    }

    private String stateAttributeName(SocialProvider provider) {
        return OAUTH_STATE_SESSION_ATTRIBUTE_PREFIX + provider.name();
    }

    private String redirectUriAttributeName(SocialProvider provider) {
        return OAUTH_REDIRECT_URI_SESSION_ATTRIBUTE_PREFIX + provider.name();
    }

    private record VerifiedAuthorization(String state, String redirectUri) {}
}
