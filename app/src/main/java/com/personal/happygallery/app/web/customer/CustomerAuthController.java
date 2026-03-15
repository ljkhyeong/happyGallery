package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.domain.user.User;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CustomerAuthController {

    private static final String COOKIE_NAME = CustomerAuthFilter.COOKIE_NAME;
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7일

    private final CustomerAuthService customerAuthService;

    public CustomerAuthController(CustomerAuthService customerAuthService) {
        this.customerAuthService = customerAuthService;
    }

    @PostMapping("/auth/signup")
    public ResponseEntity<MeResponse> signup(@RequestBody @Valid SignupRequest request,
                                             HttpServletResponse response) {
        CustomerAuthService.TokenResult result = customerAuthService.signup(
                request.email(), request.password(), request.name(), request.phone());
        addSessionCookie(response, result.rawToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(toMeResponse(result.user()));
    }

    @PostMapping("/auth/login")
    public ResponseEntity<MeResponse> login(@RequestBody @Valid LoginRequest request,
                                            HttpServletResponse response) {
        CustomerAuthService.TokenResult result = customerAuthService.login(
                request.email(), request.password());
        addSessionCookie(response, result.rawToken());
        return ResponseEntity.ok(toMeResponse(result.user()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = COOKIE_NAME, required = false) String token,
            HttpServletResponse response) {
        if (token != null && !token.isBlank()) {
            customerAuthService.logout(token);
        }
        clearSessionCookie(response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(HttpServletRequest request) {
        User user = (User) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ATTR);
        return ResponseEntity.ok(toMeResponse(user));
    }

    private void addSessionCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(COOKIE_NAME, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(cookie);
    }

    private void clearSessionCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private static MeResponse toMeResponse(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getName(),
                user.getPhone(), user.isPhoneVerified());
    }

    public record SignupRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @NotBlank String name,
            @NotBlank String phone) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record MeResponse(Long id, String email, String name, String phone, boolean phoneVerified) {}
}
