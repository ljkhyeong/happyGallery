package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.customer.GuestClaimService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/guest-claims")
public class MeGuestClaimController {

    private final GuestClaimService guestClaimService;

    public MeGuestClaimController(GuestClaimService guestClaimService) {
        this.guestClaimService = guestClaimService;
    }

    @GetMapping("/preview")
    public GuestClaimService.ClaimPreview previewGuestClaims(HttpServletRequest request) {
        return guestClaimService.preview(getUserId(request));
    }

    @PostMapping("/verify")
    public GuestClaimService.ClaimPreview verifyPhoneAndPreviewGuestClaims(
            @RequestBody @Valid VerifyGuestClaimPhoneRequest req,
            HttpServletRequest request) {
        return guestClaimService.verifyPhoneAndPreview(getUserId(request), req.verificationCode());
    }

    @PostMapping
    public GuestClaimService.ClaimResult claimGuestRecords(
            @RequestBody @Valid ClaimGuestRecordsRequest req,
            HttpServletRequest request) {
        return guestClaimService.claim(getUserId(request), req.orderIds(), req.bookingIds(), req.passIds());
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }

    // ── DTO ──

    public record VerifyGuestClaimPhoneRequest(
            @NotBlank String verificationCode) {}

    public record ClaimGuestRecordsRequest(
            List<Long> orderIds,
            List<Long> bookingIds,
            List<Long> passIds) {}
}
