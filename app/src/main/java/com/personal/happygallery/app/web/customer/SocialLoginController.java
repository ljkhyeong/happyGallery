package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.app.customer.port.in.SocialAuthUseCase.AuthorizationUrlResult;
import com.personal.happygallery.app.customer.port.in.SocialAuthUseCase.SocialLoginResult;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.app.web.customer.dto.GoogleAuthUrlResponse;
import com.personal.happygallery.app.web.customer.dto.SocialLoginRequest;
import com.personal.happygallery.app.web.customer.dto.SocialLoginResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    public SocialLoginController(SocialAuthUseCase socialAuth) {
        this.socialAuth = socialAuth;
    }

    @GetMapping("/google/url")
    public GoogleAuthUrlResponse googleAuthUrl(@RequestParam String redirectUri) {
        AuthorizationUrlResult result = socialAuth.buildAuthorizationUrl(redirectUri);
        return new GoogleAuthUrlResponse(result.url(), result.state());
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
