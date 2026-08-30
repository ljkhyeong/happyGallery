package com.personal.happygallery.adapter.in.web.policy.dto;

import com.personal.happygallery.application.policy.PolicyAcceptance;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record PolicyAcceptanceRequest(
        @NotBlank String termsVersion,
        @AssertTrue
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean termsAccepted,
        @NotBlank String privacyVersion,
        @AssertTrue
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        boolean privacyAccepted
) {

    public PolicyAcceptance toCommand() {
        return new PolicyAcceptance(
                termsVersion, termsAccepted, privacyVersion, privacyAccepted);
    }
}
