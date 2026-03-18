package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.customer.port.in.CustomerAuthUseCase;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.CustomerLoginRequest;
import com.personal.happygallery.app.web.customer.dto.MeResponse;
import com.personal.happygallery.app.web.customer.dto.SignupRequest;
import com.personal.happygallery.domain.user.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CustomerAuthController {

    private final CustomerAuthUseCase customerAuth;

    public CustomerAuthController(CustomerAuthUseCase customerAuth) {
        this.customerAuth = customerAuth;
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<MeResponse> signup(@RequestBody @Valid SignupRequest request,
                                             HttpServletRequest httpRequest) {
        User user = customerAuth.signup(request.email(), request.password(), request.name(), request.phone());
        httpRequest.getSession(true).setAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMeResponse(user));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<MeResponse> login(@RequestBody @Valid CustomerLoginRequest request,
                                            HttpServletRequest httpRequest) {
        User user = customerAuth.login(request.email(), request.password());
        httpRequest.getSession(true).setAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR, user.getId());
        return ResponseEntity.ok(toMeResponse(user));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ATTR);
        return ResponseEntity.ok(toMeResponse(user));
    }

    private static MeResponse toMeResponse(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getName(),
                user.getPhone(), user.isPhoneVerified());
    }
}
