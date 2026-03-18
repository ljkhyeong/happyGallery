package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.web.admin.dto.LoginRequest;
import com.personal.happygallery.app.web.admin.dto.LoginResponse;
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

    private final AdminAuthService adminAuthService;

    public AdminLoginController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody @Valid LoginRequest request) {
        return new LoginResponse(adminAuthService.login(request.username(), request.password()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            adminAuthService.logout(authHeader.substring(7));
        }
    }
}
