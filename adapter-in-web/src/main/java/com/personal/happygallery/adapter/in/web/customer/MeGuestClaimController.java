package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.ClaimGuestRecordsRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.VerifyGuestClaimPhoneRequest;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/guest-claims")
public class MeGuestClaimController {

    private final GuestClaimUseCase guestClaim;

    public MeGuestClaimController(GuestClaimUseCase guestClaim) {
        this.guestClaim = guestClaim;
    }

    @GetMapping("/preview")
    public GuestClaimUseCase.ClaimPreview previewGuestClaims(@CustomerUserId Long userId) {
        return guestClaim.preview(userId);
    }

    @PostMapping("/verify")
    public GuestClaimUseCase.ClaimPreview verifyPhoneAndPreviewGuestClaims(
            @RequestBody @Valid VerifyGuestClaimPhoneRequest req,
            @CustomerUserId Long userId) {
        return guestClaim.verifyPhoneAndPreview(userId, req.verificationCode());
    }

    @PostMapping
    public GuestClaimUseCase.ClaimResult claimGuestRecords(
            @RequestBody @Valid ClaimGuestRecordsRequest req,
            @CustomerUserId Long userId) {
        return guestClaim.claim(userId, req.orderIds(), req.bookingIds());
    }
}
