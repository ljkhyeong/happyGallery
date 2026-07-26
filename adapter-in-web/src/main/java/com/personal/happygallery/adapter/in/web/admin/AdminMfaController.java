package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaCodeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaDisableRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaEnrollmentResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaRecoveryCodesResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaStatusResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth/mfa")
public class AdminMfaController {

    private final AdminMfaUseCase adminMfaUseCase;

    public AdminMfaController(AdminMfaUseCase adminMfaUseCase) {
        this.adminMfaUseCase = adminMfaUseCase;
    }

    @GetMapping
    @Operation(operationId = "getAdminMfaStatus")
    public AdminMfaStatusResponse getStatus(
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminMfaStatusResponse.from(
                adminMfaUseCase.getStatus(admin.requireBearerAdminUserId()));
    }

    @PostMapping("/enrollment")
    @Operation(operationId = "beginAdminMfaEnrollment")
    public AdminMfaEnrollmentResponse beginEnrollment(
            @AuthenticationPrincipal AdminPrincipal admin,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return AdminMfaEnrollmentResponse.from(
                adminMfaUseCase.beginEnrollment(admin.requireBearerAdminUserId()));
    }

    @PostMapping("/enrollment/confirm")
    @Operation(operationId = "confirmAdminMfaEnrollment")
    public AdminMfaRecoveryCodesResponse confirmEnrollment(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestBody @Valid AdminMfaCodeRequest request,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return AdminMfaRecoveryCodesResponse.from(
                adminMfaUseCase.confirmEnrollment(
                        admin.requireBearerAdminUserId(), request.code()));
    }

    @DeleteMapping
    @Operation(operationId = "disableAdminMfa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestBody @Valid AdminMfaDisableRequest request) {
        adminMfaUseCase.disable(
                admin.requireBearerAdminUserId(), request.currentPassword(), request.code());
    }
}
