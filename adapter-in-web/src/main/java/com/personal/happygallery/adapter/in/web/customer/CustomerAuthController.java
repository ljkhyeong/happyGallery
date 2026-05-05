package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import com.personal.happygallery.adapter.in.web.customer.dto.CustomerLoginRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.SignupRequest;
import com.personal.happygallery.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
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
    private final AuthSessionWriter authSessionWriter;

    public CustomerAuthController(CustomerAuthUseCase customerAuth,
                                  AuthSessionWriter authSessionWriter) {
        this.customerAuth = customerAuth;
        this.authSessionWriter = authSessionWriter;
    }

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerUserResponse signup(@RequestBody @Valid SignupRequest request,
                                       HttpServletRequest httpRequest) {
        User user = customerAuth.signup(
                new CustomerAuthUseCase.SignupCommand(
                        request.email(),
                        request.password(),
                        request.name(),
                        request.phone()));
        authSessionWriter.bind(httpRequest, user.getId());
        return CustomerUserResponse.from(user);
    }

    @PostMapping("/auth/login")
    public CustomerUserResponse login(@RequestBody @Valid CustomerLoginRequest request,
                                      HttpServletRequest httpRequest) {
        User user = customerAuth.login(
                new CustomerAuthUseCase.LoginCommand(
                        request.email(),
                        request.password()));
        authSessionWriter.bind(httpRequest, user.getId());
        return CustomerUserResponse.from(user);
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @GetMapping("/me")
    public CustomerUserResponse me(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ATTR);
        return CustomerUserResponse.from(user);
    }
}
