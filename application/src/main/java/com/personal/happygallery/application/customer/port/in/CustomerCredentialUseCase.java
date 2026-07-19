package com.personal.happygallery.application.customer.port.in;

public interface CustomerCredentialUseCase {

    void changePassword(ChangePasswordCommand command);

    Long resetPassword(ResetPasswordCommand command);

    record ChangePasswordCommand(Long userId, String currentPassword, String newPassword) {
    }

    record ResetPasswordCommand(String email, String phone,
                                String verificationCode, String newPassword) {
    }

}
