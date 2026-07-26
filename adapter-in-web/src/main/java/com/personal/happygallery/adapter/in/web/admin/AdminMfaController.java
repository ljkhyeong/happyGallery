package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaCodeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaDisableRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaEnrollmentResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaRecoveryCodesResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminMfaStatusResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.admin.port.in.AdminMfaUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
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
                adminMfaUseCase.getStatus(requireBearerAdminId(admin)));
    }

    @PostMapping("/enrollment")
    @Operation(operationId = "beginAdminMfaEnrollment")
    public AdminMfaEnrollmentResponse beginEnrollment(
            @AuthenticationPrincipal AdminPrincipal admin,
            HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return AdminMfaEnrollmentResponse.from(
                adminMfaUseCase.beginEnrollment(requireBearerAdminId(admin)));
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
                        requireBearerAdminId(admin), request.code()));
    }

    @DeleteMapping
    @Operation(operationId = "disableAdminMfa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestBody @Valid AdminMfaDisableRequest request) {
        adminMfaUseCase.disable(
                requireBearerAdminId(admin), request.currentPassword(), request.code());
    }

    private static Long requireBearerAdminId(AdminPrincipal admin) {
        if (admin == null
                || admin.authenticationSource() != AdminPrincipal.AuthenticationSource.BEARER_SESSION
                || admin.adminUserId() == null) {
            throw new HappyGalleryException(
                    ErrorCode.FORBIDDEN, "Bearer 관리자 세션에서만 MFA를 관리할 수 있습니다.");
        }
        return admin.adminUserId();
    }
}
