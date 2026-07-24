package com.personal.happygallery.adapter.in.web.policy;

import com.personal.happygallery.adapter.in.web.policy.dto.CurrentPolicyConsentResponse;
import com.personal.happygallery.application.policy.PolicyConsentService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
public class PolicyConsentController {

    private final PolicyConsentService policyConsentService;

    public PolicyConsentController(PolicyConsentService policyConsentService) {
        this.policyConsentService = policyConsentService;
    }

    @GetMapping("/current")
    @Operation(operationId = "getCurrentPolicyConsent")
    public CurrentPolicyConsentResponse current() {
        return CurrentPolicyConsentResponse.from(policyConsentService.currentPolicy());
    }
}
