package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaVerificationRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.LoginRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.LoginResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminBearerTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminLoginController {

    private final AdminAuthUseCase adminAuthUseCase;
    private final AdminBearerTokenResolver bearerTokenResolver;

    public AdminLoginController(AdminAuthUseCase adminAuthUseCase,
                                AdminBearerTokenResolver bearerTokenResolver) {
        this.adminAuthUseCase = adminAuthUseCase;
        this.bearerTokenResolver = bearerTokenResolver;
    }

    @PostMapping("/login")
    @Operation(operationId = "adminLogin")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return LoginResponse.from(adminAuthUseCase.login(request.username(), request.password()));
    }

    @PostMapping("/mfa/verify")
    @Operation(operationId = "verifyAdminMfa")
    public LoginResponse verifyMfa(@RequestBody @Valid AdminMfaVerificationRequest request) {
        return LoginResponse.from(
                adminAuthUseCase.verifyMfa(request.challengeToken(), request.code()));
    }

    @PostMapping("/logout")
    @Operation(operationId = "adminLogout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        AdminBearerTokenResolver.Resolution bearer = bearerTokenResolver.resolve(authHeader);
        if (bearer.hasToken()) {
            adminAuthUseCase.logout(bearer.token());
        }
    }
}
