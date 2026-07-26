package com.personal.happygallery.application.admin.port.in;

import java.util.List;

public interface AdminMfaUseCase {

    MfaStatus getStatus(Long adminUserId);

    MfaEnrollment beginEnrollment(Long adminUserId);

    RecoveryCodes confirmEnrollment(Long adminUserId, String code);

    void disable(Long adminUserId, String currentPassword, String code);

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
