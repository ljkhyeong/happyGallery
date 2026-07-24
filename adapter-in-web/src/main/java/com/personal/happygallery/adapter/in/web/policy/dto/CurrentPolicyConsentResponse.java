package com.personal.happygallery.adapter.in.web.policy.dto;

import com.personal.happygallery.application.policy.PolicyConsentService.CurrentPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

public record CurrentPolicyConsentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PolicyDocument terms,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PolicyDocument privacy
) {

    private static final String TERMS_PATH = "/terms";
    private static final String PRIVACY_PATH = "/privacy";

    public static CurrentPolicyConsentResponse from(CurrentPolicy policy) {
        return new CurrentPolicyConsentResponse(
                new PolicyDocument(policy.termsVersion(), TERMS_PATH),
                new PolicyDocument(policy.privacyVersion(), PRIVACY_PATH));
    }

    public record PolicyDocument(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String version,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String documentPath
    ) {}
}
