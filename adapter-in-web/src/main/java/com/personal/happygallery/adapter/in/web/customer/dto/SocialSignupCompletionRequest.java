package com.personal.happygallery.adapter.in.web.customer.dto;

import com.personal.happygallery.adapter.in.web.policy.dto.PolicyAcceptanceRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SocialSignupCompletionRequest(
        @NotBlank @Size(max = 36) String attemptId,
        @NotNull @Valid PolicyAcceptanceRequest policyAcceptance
) {}
