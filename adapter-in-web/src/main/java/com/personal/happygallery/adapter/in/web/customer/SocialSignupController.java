package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.SocialSignupAuthorizationResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialSignupCompletionRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CustomerUserResponse;
import com.personal.happygallery.adapter.in.web.security.customer.PendingSocialSignupStore;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import jakarta.servlet.http.HttpServletResponse;
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
@RequestMapping("/api/v1/auth/social")
public class SocialSignupController {

    private final PolicyConsentService policyConsentService;
    private final SocialSignupIntentStore signupIntentStore;
    private final PendingSocialSignupStore pendingSignupStore;
    private final SocialAuthUseCase socialAuth;
    private final CustomerSessionBinder sessionBinder;

    public SocialSignupController(PolicyConsentService policyConsentService,
                                  SocialSignupIntentStore signupIntentStore,
                                  PendingSocialSignupStore pendingSignupStore,
                                  SocialAuthUseCase socialAuth,
                                  CustomerSessionBinder sessionBinder) {
        this.policyConsentService = policyConsentService;
        this.signupIntentStore = signupIntentStore;
        this.pendingSignupStore = pendingSignupStore;
        this.socialAuth = socialAuth;
        this.sessionBinder = sessionBinder;
    }

    @Operation(operationId = "startSocialSignup")
    @PostMapping("/signup-intents/{provider}")
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

    @Operation(operationId = "completeSocialSignup")
    @PostMapping("/signup-completion")
    public CustomerUserResponse complete(
            @RequestBody @Valid SocialSignupCompletionRequest completion,
            HttpServletRequest request,
            HttpServletResponse response) {
        var acceptance = completion.policyAcceptance().toCommand();
        policyConsentService.requireCurrent(acceptance);
        var profile = pendingSignupStore.consume(request, completion.attemptId());
        var result = socialAuth.socialLogin(profile.withPolicyAcceptance(acceptance));
        sessionBinder.bind(request, response, result.user());
        return CustomerUserResponse.from(result.user());
    }

}
