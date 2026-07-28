package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.CustomerLoginRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.SignupRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import com.personal.happygallery.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CustomerAuthController {

    private final CustomerAuthUseCase customerAuth;
    private final CustomerSessionBinder customerSessionBinder;
    private final SubjectRateLimitGuard rateLimitGuard;

    public CustomerAuthController(CustomerAuthUseCase customerAuth,
                                  CustomerSessionBinder customerSessionBinder,
                                  SubjectRateLimitGuard rateLimitGuard) {
        this.customerAuth = customerAuth;
        this.customerSessionBinder = customerSessionBinder;
        this.rateLimitGuard = rateLimitGuard;
    }

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerUserResponse signup(@RequestBody @Valid SignupRequest request,
                                       HttpServletRequest httpRequest,
                                       HttpServletResponse httpResponse) {
        User user = customerAuth.signup(
                new CustomerAuthUseCase.SignupCommand(
                        request.email(),
                        request.password(),
                        request.name(),
                        request.phone(),
                        request.verificationCode(),
                        request.policyAcceptance().toCommand()));
        customerSessionBinder.bind(httpRequest, httpResponse, user);
        return CustomerUserResponse.from(user);
    }

    @PostMapping("/auth/login")
    public CustomerUserResponse login(@RequestBody @Valid CustomerLoginRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse httpResponse) {
        rateLimitGuard.checkCustomerLogin(request.email());
        User user = customerAuth.login(
                new CustomerAuthUseCase.LoginCommand(
                        request.email(),
                        request.password()));
        customerSessionBinder.bind(httpRequest, httpResponse, user);
        return CustomerUserResponse.from(user);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        customerSessionBinder.unbind(httpRequest, httpResponse);
    }

    @GetMapping("/me")
    public CustomerUserResponse me(@AuthenticationPrincipal CustomerPrincipal customer) {
        return new CustomerUserResponse(
                customer.userId(),
                customer.email(),
                customer.name(),
                customer.phone(),
                customer.phoneVerified(),
                customer.localPasswordEnabled()
        );
    }
}
