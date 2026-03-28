package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.app.customer.port.in.SocialAuthUseCase.SocialLoginResult;
import com.personal.happygallery.app.customer.port.out.OAuthTokenExchangePort;
import com.personal.happygallery.app.customer.port.out.OAuthTokenExchangePort.AuthorizationUrl;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.app.web.customer.dto.SocialLoginRequest;
import com.personal.happygallery.app.web.customer.dto.SocialLoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/social")
public class SocialLoginController {

    private final SocialAuthUseCase socialAuth;
    private final OAuthTokenExchangePort oauthPort;

    public SocialLoginController(SocialAuthUseCase socialAuth,
                                 OAuthTokenExchangePort oauthPort) {
        this.socialAuth = socialAuth;
        this.oauthPort = oauthPort;
    }

    @GetMapping("/google/url")
    public Map<String, String> googleAuthUrl(@RequestParam String redirectUri) {
        String state = UUID.randomUUID().toString();
        AuthorizationUrl authUrl = oauthPort.buildAuthorizationUrl(redirectUri, state);
        return Map.of("url", authUrl.url(), "state", authUrl.state());
    }

    @PostMapping("/google")
    public SocialLoginResponse googleLogin(@RequestBody @Valid SocialLoginRequest request,
                                           HttpServletRequest httpRequest) {
        SocialLoginResult result = socialAuth.socialLogin(
                new SocialAuthUseCase.SocialLoginCommand(request.code(), request.redirectUri()));
        httpRequest.getSession(true)
                .setAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR, result.user().getId());
        return new SocialLoginResponse(
                CustomerUserResponse.from(result.user()),
                result.newUser());
    }
}
