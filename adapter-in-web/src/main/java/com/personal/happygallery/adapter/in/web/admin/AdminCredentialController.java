package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminPasswordChangeRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.admin.port.in.AdminCredentialUseCase;
import com.personal.happygallery.application.admin.port.in.AdminCredentialUseCase.ChangePasswordCommand;
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
        adminCredentialUseCase.changePassword(new ChangePasswordCommand(
                admin.requireBearerAdminUserId(),
                request.currentPassword(),
                request.newPassword()));
    }
}
