package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.customer.port.in.GuestClaimUseCase;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.ClaimGuestRecordsRequest;
import com.personal.happygallery.app.web.customer.dto.VerifyGuestClaimPhoneRequest;
import jakarta.servlet.http.HttpServletRequest;
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
    public GuestClaimUseCase.ClaimPreview previewGuestClaims(HttpServletRequest request) {
        return guestClaim.preview(getUserId(request));
    }

    @PostMapping("/verify")
    public GuestClaimUseCase.ClaimPreview verifyPhoneAndPreviewGuestClaims(
            @RequestBody @Valid VerifyGuestClaimPhoneRequest req,
            HttpServletRequest request) {
        return guestClaim.verifyPhoneAndPreview(getUserId(request), req.verificationCode());
    }

    @PostMapping
    public GuestClaimUseCase.ClaimResult claimGuestRecords(
            @RequestBody @Valid ClaimGuestRecordsRequest req,
            HttpServletRequest request) {
        return guestClaim.claim(getUserId(request), req.orderIds(), req.bookingIds());
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}
