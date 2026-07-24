package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminPasswordChangeRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.admin.port.in.AdminCredentialUseCase;
import com.personal.happygallery.application.admin.port.in.AdminCredentialUseCase.ChangePasswordCommand;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminCredentialController {

    private final AdminCredentialUseCase adminCredentialUseCase;

    public AdminCredentialController(AdminCredentialUseCase adminCredentialUseCase) {
        this.adminCredentialUseCase = adminCredentialUseCase;
    }

    @PatchMapping("/password")
    @Operation(operationId = "changeAdminPassword")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(
            @AuthenticationPrincipal AdminPrincipal admin,
            @RequestBody @Valid AdminPasswordChangeRequest request) {
        if (admin == null
                || admin.authenticationSource() != AdminPrincipal.AuthenticationSource.BEARER_SESSION
                || admin.adminUserId() == null) {
            throw new HappyGalleryException(
                    ErrorCode.FORBIDDEN, "Bearer 관리자 세션에서만 비밀번호를 변경할 수 있습니다.");
        }
        adminCredentialUseCase.changePassword(new ChangePasswordCommand(
                admin.adminUserId(), request.currentPassword(), request.newPassword()));
    }
}
