package com.personal.happygallery.application.admin.port.in;

public interface AdminCredentialUseCase {

    void changePassword(ChangePasswordCommand command);

    record ChangePasswordCommand(
            Long adminUserId,
            String currentPassword,
            String newPassword
    ) {}
}
