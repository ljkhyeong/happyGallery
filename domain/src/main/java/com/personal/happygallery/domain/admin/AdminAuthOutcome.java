package com.personal.happygallery.domain.admin;

public enum AdminAuthOutcome {
    LOGIN_SUCCEEDED,
    LOGIN_FAILED,
    LOGIN_BLOCKED,
    MFA_REQUIRED,
    MFA_FAILED,
    MFA_ENABLED,
    MFA_DISABLED,
    RECOVERY_CODE_USED
}
