package com.personal.happygallery.adapter.in.web.booking;

import com.personal.happygallery.application.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.application.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.application.customer.GuestPersonalDataProtector;
import com.personal.happygallery.adapter.in.web.booking.dto.BookingDetailResponse;
import com.personal.happygallery.adapter.in.web.booking.dto.CancelResponse;
import com.personal.happygallery.adapter.in.web.booking.dto.RescheduleRequest;
import com.personal.happygallery.adapter.in.web.booking.dto.RescheduleResponse;
import com.personal.happygallery.adapter.in.web.booking.dto.ReduceBookingParticipantsRequest;
import com.personal.happygallery.adapter.in.web.booking.dto.ReduceBookingParticipantsResponse;
import com.personal.happygallery.adapter.in.web.booking.dto.SendVerificationRequest;
import com.personal.happygallery.adapter.in.web.booking.dto.SendVerificationResponse;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.PhoneVerification;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.Clock;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 예약 조회/변경/취소 API.
 *
 * <p>예약 생성은 {@code POST /api/v1/payments/prepare} → {@code /confirm} 경로로 일원화됨.
 */
@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final GuestBookingUseCase guestBookingUseCase;
    private final BookingQueryUseCase bookingQueryUseCase;
    private final BookingRescheduleUseCase bookingRescheduleUseCase;
    private final BookingCancelUseCase bookingCancelUseCase;
    private final GuestPersonalDataProtector guestPersonalDataProtector;
    private final SubjectRateLimitGuard rateLimitGuard;
    private final Clock clock;

    public BookingController(GuestBookingUseCase guestBookingUseCase,
                             BookingQueryUseCase bookingQueryUseCase,
                             BookingRescheduleUseCase bookingRescheduleUseCase,
                             BookingCancelUseCase bookingCancelUseCase,
                             GuestPersonalDataProtector guestPersonalDataProtector,
                             SubjectRateLimitGuard rateLimitGuard,
                             Clock clock) {
        this.guestBookingUseCase = guestBookingUseCase;
        this.bookingQueryUseCase = bookingQueryUseCase;
        this.bookingRescheduleUseCase = bookingRescheduleUseCase;
        this.bookingCancelUseCase = bookingCancelUseCase;
        this.guestPersonalDataProtector = guestPersonalDataProtector;
        this.rateLimitGuard = rateLimitGuard;
        this.clock = clock;
    }

    /** 휴대폰 인증 코드 발송. 응답에는 인증 코드를 포함하지 않는다. */
    @PostMapping("/phone-verifications")
    @Operation(operationId = "sendGuestBookingVerification")
    public SendVerificationResponse sendVerification(
            @RequestBody @Valid SendVerificationRequest request) {
        rateLimitGuard.checkPhoneVerification(request.phone());
        PhoneVerification pv = guestBookingUseCase.sendVerificationCode(
                request.phone(), request.purpose());
        return SendVerificationResponse.from(pv);
    }

    /** 비회원 예약 조회 — bookingId + X-Access-Token 헤더 검증 */
    @GetMapping("/{bookingId}")
    @Operation(operationId = "getGuestBooking")
    public BookingDetailResponse getBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-Access-Token") String token) {
        BookingQueryUseCase.BookingDetail detail = bookingQueryUseCase.getBookingByToken(bookingId, token);
        Booking booking = detail.booking();
        return BookingDetailResponse.from(
                booking,
                detail.refund(),
                guestPersonalDataProtector.decryptName(booking.getGuest()),
                guestPersonalDataProtector.decryptPhone(booking.getGuest()),
                clock);
    }

    /** 비회원 예약 변경 — 슬롯 교체, 이력 누적 */
    @PatchMapping("/{bookingId}/reschedule")
    @Operation(operationId = "rescheduleGuestBooking")
    public RescheduleResponse reschedule(
            @PathVariable Long bookingId,
            @RequestHeader("X-Access-Token") String token,
            @RequestBody @Valid RescheduleRequest request) {
        Booking booking = bookingRescheduleUseCase.rescheduleBooking(
                bookingId, token, request.newSlotId());
        return RescheduleResponse.from(booking);
    }

    /** 환불 가능 기간의 다인 예약에서 일부 인원만 취소한다. */
    @PatchMapping("/{bookingId}/participants")
    @Operation(operationId = "reduceGuestBookingParticipants")
    public ReduceBookingParticipantsResponse reduceParticipants(
            @PathVariable Long bookingId,
            @RequestHeader("X-Access-Token") String token,
            @RequestBody @Valid ReduceBookingParticipantsRequest request) {
        return ReduceBookingParticipantsResponse.from(
                bookingCancelUseCase.reduceGuestBookingParticipants(
                        bookingId, token, request.participantCount()));
    }

    /** 비회원 예약 취소 — CANCELED 전이, D-1 이전이면 환불 요청 기록 */
    @DeleteMapping("/{bookingId}")
    @Operation(operationId = "cancelGuestBooking")
    public CancelResponse cancelBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-Access-Token") String token) {
        BookingCancelUseCase.CancelResult result =
                bookingCancelUseCase.cancelBooking(bookingId, token);
        return CancelResponse.from(result);
    }
}
