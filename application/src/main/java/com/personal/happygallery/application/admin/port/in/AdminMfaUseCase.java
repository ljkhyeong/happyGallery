package com.personal.happygallery.application.admin.port.in;

import com.personal.happygallery.application.admin.port.AdminAuthenticationMethod;
import java.util.List;

public interface AdminMfaUseCase {

    MfaStatus getStatus(Long adminUserId);

    MfaEnrollment beginEnrollment(Long adminUserId);

    RecoveryCodes confirmEnrollment(Long adminUserId, String code);

    void disable(Long adminUserId, String currentPassword, String code);

    void recover(
            Long adminUserId,
            String currentPassword,
            AdminAuthenticationMethod authenticationMethod);

    record MfaStatus(
            boolean enabled,
            boolean enrollmentPending,
            long recoveryCodesRemaining
    ) {}

    record MfaEnrollment(String secret, String provisioningUri) {}

    record RecoveryCodes(List<String> recoveryCodes) {
        public RecoveryCodes {
            recoveryCodes = List.copyOf(recoveryCodes);
        }
    }
}
