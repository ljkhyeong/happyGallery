package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialAuthUrlResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialLoginRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialLoginResponse;
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
import org.springframework.util.StringUtils;
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

    private final SocialAuthUseCase socialAuth;
    private final CustomerSessionBinder customerSessionBinder;

    public SocialLoginController(SocialAuthUseCase socialAuth,
                                 CustomerSessionBinder customerSessionBinder) {
        this.socialAuth = socialAuth;
        this.customerSessionBinder = customerSessionBinder;
    }

    @GetMapping("/{provider}/url")
    public ResponseEntity<SocialAuthUrlResponse> authorizationUrl(@PathVariable String provider,
                                                                  @RequestParam String redirectUri,
                                                                  HttpServletRequest httpRequest) {
        SocialProvider socialProvider = SocialProvider.fromPath(provider);
        AuthorizationUrlResult result = socialAuth.buildAuthorizationUrl(
                socialProvider, redirectUri);
        storeState(httpRequest, socialProvider, result.state());
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
        String verifiedState = verifyAndConsumeState(httpRequest, socialProvider, request.state());
        SocialLoginResult result = socialAuth.socialLogin(
                new SocialAuthUseCase.SocialLoginCommand(
                        socialProvider,
                        request.code(),
                        request.redirectUri(),
                        verifiedState));
        customerSessionBinder.bind(httpRequest, httpResponse, result.user().getId());
        return new SocialLoginResponse(
                CustomerUserResponse.from(result.user()),
                result.newUser());
    }

    private void storeState(HttpServletRequest request, SocialProvider provider, String state) {
        request.getSession().setAttribute(stateAttributeName(provider), state);
    }

    private String verifyAndConsumeState(HttpServletRequest request,
                                         SocialProvider provider,
                                         String actualState) {
        HttpSession session = request.getSession(false);

        // state를 보내지 않던 구 Google 콜백과의 롤링 배포 호환 분기다.
        if (provider == SocialProvider.GOOGLE && !StringUtils.hasText(actualState)) {
            if (session != null) {
                session.removeAttribute(stateAttributeName(provider));
            }
            return null;
        }

        if (session == null) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }

        String attributeName = stateAttributeName(provider);
        Object expectedState = session.getAttribute(attributeName);
        session.removeAttribute(attributeName);

        if (!(expectedState instanceof String state) || !state.equals(actualState)) {
            throw new HappyGalleryException(ErrorCode.SOCIAL_LOGIN_FAILED);
        }
        return actualState;
    }

    private String stateAttributeName(SocialProvider provider) {
        return OAUTH_STATE_SESSION_ATTRIBUTE_PREFIX + provider.name();
    }
}
