package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.GuestRecordRecoveryResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.PaymentStatusRecoveryResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.RecoverGuestRecordsRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.RecoverPaymentStatusesRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/guest-records")
public class GuestRecordRecoveryController {

    private final GuestRecordRecoveryUseCase guestRecordRecovery;
    private final PaymentStatusRecoveryUseCase paymentStatusRecovery;
    private final SubjectRateLimitGuard rateLimitGuard;

    public GuestRecordRecoveryController(GuestRecordRecoveryUseCase guestRecordRecovery,
                                         PaymentStatusRecoveryUseCase paymentStatusRecovery,
                                         SubjectRateLimitGuard rateLimitGuard) {
        this.guestRecordRecovery = guestRecordRecovery;
        this.paymentStatusRecovery = paymentStatusRecovery;
        this.rateLimitGuard = rateLimitGuard;
    }

    @Operation(operationId = "recoverGuestRecords")
    @PostMapping("/recovery")
    public GuestRecordRecoveryResponse recover(@RequestBody @Valid RecoverGuestRecordsRequest request) {
        rateLimitGuard.checkGuestRecordRecovery(request.phone());
        return GuestRecordRecoveryResponse.from(
                guestRecordRecovery.recover(request.phone(), request.verificationCode()));
    }

    @Operation(operationId = "recoverGuestPaymentStatuses")
    @PostMapping("/payment-status-recovery")
    public PaymentStatusRecoveryResponse recoverPaymentStatuses(
            @RequestBody @Valid RecoverPaymentStatusesRequest request,
            HttpServletResponse response) {
        rateLimitGuard.checkGuestRecordRecovery(request.phone());
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        return PaymentStatusRecoveryResponse.from(
                paymentStatusRecovery.recover(request.phone(), request.verificationCode()));
    }
}
