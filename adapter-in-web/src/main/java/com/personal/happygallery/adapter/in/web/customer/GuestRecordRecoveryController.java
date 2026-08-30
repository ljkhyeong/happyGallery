package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.GuestRecordRecoveryResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.GuestRecoveredBookingPageResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.GuestRecoveredOrderPageResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.PaymentStatusRecoveryResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.RecoverGuestRecordsRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.RecoverPaymentStatusesRequest;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.application.customer.port.in.GuestRecordRecoveryUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentStatusRecoveryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(operationId = "listRecoveredGuestOrders")
    @GetMapping("/recovery/orders")
    public GuestRecoveredOrderPageResponse listRecoveredOrders(
            @RequestHeader("X-Access-Token") String accessToken,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return GuestRecoveredOrderPageResponse.from(
                guestRecordRecovery.listRecoveredOrders(accessToken, cursor, size));
    }

    @Operation(operationId = "listRecoveredGuestBookings")
    @GetMapping("/recovery/bookings")
    public GuestRecoveredBookingPageResponse listRecoveredBookings(
            @RequestHeader("X-Access-Token") String accessToken,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return GuestRecoveredBookingPageResponse.from(
                guestRecordRecovery.listRecoveredBookings(accessToken, cursor, size));
    }

    @Operation(operationId = "recoverGuestPaymentStatuses")
    @PostMapping("/payment-status-recovery")
    public PaymentStatusRecoveryResponse recoverPaymentStatuses(
            @RequestBody @Valid RecoverPaymentStatusesRequest request) {
        rateLimitGuard.checkGuestRecordRecovery(request.phone());
        return PaymentStatusRecoveryResponse.from(
                paymentStatusRecovery.recover(request.phone(), request.verificationCode()));
    }
}
