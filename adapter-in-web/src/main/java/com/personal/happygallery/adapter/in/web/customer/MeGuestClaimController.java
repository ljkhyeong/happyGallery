package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.ClaimGuestRecordsRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.GuestClaimPreviewResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.GuestClaimResultResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.VerifyGuestClaimPhoneRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/guest-claims")
public class MeGuestClaimController {

    private final GuestClaimUseCase guestClaim;
    private final SubjectRateLimitGuard rateLimitGuard;

    public MeGuestClaimController(GuestClaimUseCase guestClaim,
                                  SubjectRateLimitGuard rateLimitGuard) {
        this.guestClaim = guestClaim;
        this.rateLimitGuard = rateLimitGuard;
    }

    @GetMapping("/preview")
    @Operation(operationId = "previewGuestClaims")
    public GuestClaimPreviewResponse previewGuestClaims(
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return GuestClaimPreviewResponse.from(guestClaim.preview(customer.userId()));
    }

    @PostMapping("/verify")
    @Operation(operationId = "verifyPhoneAndPreviewGuestClaims")
    public GuestClaimPreviewResponse verifyPhoneAndPreviewGuestClaims(
            @RequestBody @Valid VerifyGuestClaimPhoneRequest req,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkGuestClaim(customer.userId());
        return GuestClaimPreviewResponse.from(
                guestClaim.verifyPhoneAndPreview(customer.userId(), req.verificationCode()));
    }

    @PostMapping
    @Operation(operationId = "claimGuestRecords")
    public GuestClaimResultResponse claimGuestRecords(
            @RequestBody @Valid ClaimGuestRecordsRequest req,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return GuestClaimResultResponse.from(
                guestClaim.claim(customer.userId(), req.orderIds(), req.bookingIds()));
    }
}
