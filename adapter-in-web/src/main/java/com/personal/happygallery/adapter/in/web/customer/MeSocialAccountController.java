package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.SocialAccountAuthorizationResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.SocialAccountsResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerSecurityRoutes;
import com.personal.happygallery.adapter.in.web.security.customer.SocialAccountLinkIntentStore;
import com.personal.happygallery.application.customer.port.in.SocialAuthUseCase;
import com.personal.happygallery.domain.user.SocialProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/api/v1/me/social-accounts")
public class MeSocialAccountController {

    private final SocialAuthUseCase socialAuth;
    private final SocialAccountLinkIntentStore linkIntentStore;
    private final CustomerSessionBinder customerSessionBinder;

    public MeSocialAccountController(SocialAuthUseCase socialAuth,
                                     SocialAccountLinkIntentStore linkIntentStore,
                                     CustomerSessionBinder customerSessionBinder) {
        this.socialAuth = socialAuth;
        this.linkIntentStore = linkIntentStore;
        this.customerSessionBinder = customerSessionBinder;
    }

    @Operation(operationId = "getMySocialAccounts")
    @GetMapping
    public SocialAccountsResponse list(@AuthenticationPrincipal CustomerPrincipal customer) {
        return new SocialAccountsResponse(socialAuth.listLinkedProviders(customer.userId()));
    }

    @Operation(operationId = "startMySocialAccountLink")
    @PostMapping("/{provider}/authorization")
    public SocialAccountAuthorizationResponse startLink(
            @PathVariable
            @Parameter(schema = @Schema(allowableValues = {"google", "naver"}))
            String provider,
            @AuthenticationPrincipal CustomerPrincipal customer,
            HttpServletRequest request) {
        SocialProvider socialProvider = SocialProvider.fromPath(provider);
        String attemptId = linkIntentStore.start(
                request, customer.userId(), customer.credentialVersion(), socialProvider);
        return new SocialAccountAuthorizationResponse(
                UriComponentsBuilder.fromPath(CustomerSecurityRoutes.SOCIAL_AUTHORIZATION_BASE_URI)
                        .pathSegment(socialProvider.name().toLowerCase(Locale.ROOT))
                        .queryParam(SocialAccountLinkIntentStore.LINK_ATTEMPT_PARAMETER, attemptId)
                        .build()
                        .encode()
                        .toUriString());
    }

    @Operation(operationId = "unlinkMySocialAccount")
    @DeleteMapping("/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(
            @PathVariable
            @Parameter(schema = @Schema(allowableValues = {"google", "naver"}))
            String provider,
            @AuthenticationPrincipal CustomerPrincipal customer,
            HttpServletRequest request,
            HttpServletResponse response) {
        boolean unlinked = socialAuth.unlinkSocialAccount(
                customer.userId(), SocialProvider.fromPath(provider));
        if (unlinked) {
            customerSessionBinder.unbindIfBoundTo(request, response, customer.userId());
        }
    }
}
