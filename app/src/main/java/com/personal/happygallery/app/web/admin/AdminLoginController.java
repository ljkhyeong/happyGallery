package com.personal.happygallery.app.web.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/api/v1/admin/auth", "/admin/auth"})
public class AdminLoginController {

    private final AdminAuthService adminAuthService;

    public AdminLoginController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request) {
        return adminAuthService.login(request.username(), request.password())
                .map(token -> ResponseEntity.ok(Map.of("token", token)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("code", "UNAUTHORIZED", "message", "아이디 또는 비밀번호가 올바르지 않습니다.")));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            adminAuthService.logout(authHeader.substring(7));
        }
        return ResponseEntity.noContent().build();
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
}
