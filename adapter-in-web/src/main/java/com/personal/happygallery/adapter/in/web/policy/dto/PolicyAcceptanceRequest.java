package com.personal.happygallery.adapter.in.web.policy.dto;

import com.personal.happygallery.application.policy.PolicyAcceptance;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

public record PolicyAcceptanceRequest(
        @NotBlank String termsVersion,
        @AssertTrue boolean termsAccepted,
        @NotBlank String privacyVersion,
        @AssertTrue boolean privacyAccepted
) {

    public PolicyAcceptance toCommand() {
        return new PolicyAcceptance(
                termsVersion, termsAccepted, privacyVersion, privacyAccepted);
    }
}
