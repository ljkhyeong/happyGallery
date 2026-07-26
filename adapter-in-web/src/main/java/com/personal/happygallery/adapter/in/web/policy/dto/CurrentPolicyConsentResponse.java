package com.personal.happygallery.adapter.in.web.policy.dto;

import com.personal.happygallery.application.policy.PolicyConsentService.CurrentPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

public record CurrentPolicyConsentResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PolicyDocument terms,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) PolicyDocument privacy
) {

    public static CurrentPolicyConsentResponse from(CurrentPolicy policy) {
        return new CurrentPolicyConsentResponse(
                new PolicyDocument(
                        policy.termsVersion(),
                        "/terms/" + policy.termsVersion()),
                new PolicyDocument(
                        policy.privacyVersion(),
                        "/privacy/" + policy.privacyVersion()));
    }

    public record PolicyDocument(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String version,
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String documentPath
    ) {}
}
