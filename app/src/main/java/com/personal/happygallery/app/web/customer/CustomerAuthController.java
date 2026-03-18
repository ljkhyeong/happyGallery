package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.CustomerLoginRequest;
import com.personal.happygallery.app.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.app.web.customer.dto.SignupRequest;
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

    public CustomerAuthController(CustomerAuthUseCase customerAuth) {
        this.customerAuth = customerAuth;
    }

    @PostMapping("/auth/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public CustomerUserResponse signup(@RequestBody @Valid SignupRequest request,
                             HttpServletRequest httpRequest) {
        User user = customerAuth.signup(request.email(), request.password(), request.name(), request.phone());
        httpRequest.getSession(true).setAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR, user.getId());
        return toCustomerUserResponse(user);
    }

    @PostMapping("/auth/login")
    public CustomerUserResponse login(@RequestBody @Valid CustomerLoginRequest request,
                            HttpServletRequest httpRequest) {
        User user = customerAuth.login(request.email(), request.password());
        httpRequest.getSession(true).setAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR, user.getId());
        return toCustomerUserResponse(user);
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
        return toCustomerUserResponse(user);
    }

    private static CustomerUserResponse toCustomerUserResponse(User user) {
        return new CustomerUserResponse(user.getId(), user.getEmail(), user.getName(),
                user.getPhone(), user.isPhoneVerified());
    }
}
