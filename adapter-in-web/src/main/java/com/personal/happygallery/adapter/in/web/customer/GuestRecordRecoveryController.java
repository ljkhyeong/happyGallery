package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.GuestRecordRecoveryResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.RecoverGuestRecordsRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guest-records")
public class GuestRecordRecoveryController {

    private final GuestRecordRecoveryUseCase guestRecordRecovery;
    private final SubjectRateLimitGuard rateLimitGuard;

    public GuestRecordRecoveryController(GuestRecordRecoveryUseCase guestRecordRecovery,
                                         SubjectRateLimitGuard rateLimitGuard) {
        this.guestRecordRecovery = guestRecordRecovery;
        this.rateLimitGuard = rateLimitGuard;
    }

    @PostMapping("/recovery")
    public GuestRecordRecoveryResponse recover(@RequestBody @Valid RecoverGuestRecordsRequest request) {
        rateLimitGuard.checkGuestRecordRecovery(request.phone());
        return GuestRecordRecoveryResponse.from(
                guestRecordRecovery.recover(request.phone(), request.verificationCode()));
    }
}
