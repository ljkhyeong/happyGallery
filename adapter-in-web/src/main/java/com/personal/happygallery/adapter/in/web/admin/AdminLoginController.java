package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.admin.port.in.AdminAuthUseCase;
import com.personal.happygallery.adapter.in.web.admin.dto.LoginRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.LoginResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping({"/api/v1/admin/auth", "/admin/auth"})
public class AdminLoginController {

    private final AdminAuthUseCase adminAuthUseCase;

    public AdminLoginController(AdminAuthUseCase adminAuthUseCase) {
        this.adminAuthUseCase = adminAuthUseCase;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return new LoginResponse(adminAuthUseCase.login(request.username(), request.password()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            adminAuthUseCase.logout(authHeader.substring(7));
        }
    }
}
