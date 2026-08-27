package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.SocialSignupAuthorizationResponse;
import com.personal.happygallery.adapter.in.web.policy.dto.PolicyAcceptanceRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes;
import com.personal.happygallery.adapter.in.web.security.customer.SocialSignupIntentStore;
import com.personal.happygallery.application.policy.PolicyConsentService;
import com.personal.happygallery.domain.user.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/auth/social/signup-intents")
public class SocialSignupController {

    private final PolicyConsentService policyConsentService;
    private final SocialSignupIntentStore signupIntentStore;

    public SocialSignupController(PolicyConsentService policyConsentService,
                                  SocialSignupIntentStore signupIntentStore) {
        this.policyConsentService = policyConsentService;
        this.signupIntentStore = signupIntentStore;
    }

    @Operation(operationId = "startSocialSignup")
    @PostMapping("/{provider}")
    public SocialSignupAuthorizationResponse start(
            @PathVariable
            @Parameter(schema = @Schema(allowableValues = {"google", "naver", "kakao"}))
            String provider,
            @RequestBody @Valid PolicyAcceptanceRequest policyAcceptance,
            HttpServletRequest request) {
        SocialProvider socialProvider = SocialProvider.fromPath(provider);
        var acceptance = policyAcceptance.toCommand();
        policyConsentService.requireCurrent(acceptance);
        String attemptId = signupIntentStore.start(request, socialProvider, acceptance);
        return new SocialSignupAuthorizationResponse(
                UriComponentsBuilder.fromPath(CustomerSecurityRoutes.SOCIAL_AUTHORIZATION_BASE_URI)
                        .pathSegment(socialProvider.name().toLowerCase(Locale.ROOT))
                        .queryParam(SocialSignupIntentStore.SIGNUP_ATTEMPT_PARAMETER, attemptId)
                        .build()
                        .encode()
                        .toUriString());
    }
}
