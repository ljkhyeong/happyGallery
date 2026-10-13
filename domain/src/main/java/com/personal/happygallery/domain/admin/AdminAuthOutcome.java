package com.personal.happygallery.domain.admin;

public enum AdminAuthOutcome {
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    MFA_REQUIRED,
    MFA_FAILED,
    MFA_ENABLED,
    MFA_DISABLED,
    MFA_RECOVERY_RESET,
    RECOVERY_CODE_USED
}
