package com.personal.happygallery.app.web.booking;

import com.personal.happygallery.app.booking.port.in.BookingQueryUseCase;
import com.personal.happygallery.app.booking.port.in.BookingRescheduleUseCase;
import com.personal.happygallery.app.booking.port.in.BookingCancelUseCase;
import com.personal.happygallery.app.booking.port.in.GuestBookingUseCase;
import com.personal.happygallery.app.web.booking.dto.BookingDetailResponse;
import com.personal.happygallery.app.web.booking.dto.BookingResponse;
import com.personal.happygallery.app.web.booking.dto.CancelResponse;
import com.personal.happygallery.app.web.booking.dto.CreateGuestBookingRequest;
import com.personal.happygallery.app.web.booking.dto.RescheduleRequest;
import com.personal.happygallery.app.web.booking.dto.RescheduleResponse;
import com.personal.happygallery.app.web.booking.dto.SendVerificationRequest;
import com.personal.happygallery.app.web.booking.dto.SendVerificationResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.PhoneVerification;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/bookings", "/bookings"})
public class BookingController {

    private final GuestBookingUseCase guestBookingUseCase;
    private final BookingQueryUseCase bookingQueryUseCase;
    private final BookingRescheduleUseCase bookingRescheduleUseCase;
    private final BookingCancelUseCase bookingCancelUseCase;

    public BookingController(GuestBookingUseCase guestBookingUseCase,
                             BookingQueryUseCase bookingQueryUseCase,
                             BookingRescheduleUseCase bookingRescheduleUseCase,
                             BookingCancelUseCase bookingCancelUseCase) {
        this.guestBookingUseCase = guestBookingUseCase;
        this.bookingQueryUseCase = bookingQueryUseCase;
        this.bookingRescheduleUseCase = bookingRescheduleUseCase;
        this.bookingCancelUseCase = bookingCancelUseCase;
    }

    /** 휴대폰 인증 코드 발송 (MVP: 응답에 code 포함) */
    @PostMapping("/phone-verifications")
    public SendVerificationResponse sendVerification(
            @RequestBody @Valid SendVerificationRequest request) {
        PhoneVerification pv = guestBookingUseCase.sendVerificationCode(request.phone());
        return SendVerificationResponse.from(pv);
    }

    /** 게스트 예약 생성 */
    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createGuestBooking(
            @RequestBody @Valid CreateGuestBookingRequest request) {
        var result = guestBookingUseCase.createGuestBooking(
                request.phone(),
                request.verificationCode(),
                request.name(),
                request.slotId(),
                request.depositAmount() != null ? request.depositAmount() : 0L,
                request.paymentMethod(),
                request.passId());
        return BookingResponse.from(result.booking(), result.rawAccessToken());
    }

    /** 비회원 예약 조회 — bookingId + X-Access-Token 헤더 검증 */
    @GetMapping("/{bookingId}")
    public BookingDetailResponse getBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-Access-Token") String token) {
        Booking booking = bookingQueryUseCase.getBookingByToken(bookingId, token);
        return BookingDetailResponse.from(booking);
    }

    /** 비회원 예약 변경 — 슬롯 교체, 이력 누적 */
    @PatchMapping("/{bookingId}/reschedule")
    public RescheduleResponse reschedule(
            @PathVariable Long bookingId,
            @RequestHeader("X-Access-Token") String token,
            @RequestBody @Valid RescheduleRequest request) {
        Booking booking = bookingRescheduleUseCase.rescheduleBooking(
                bookingId, token, request.newSlotId());
        return RescheduleResponse.from(booking);
    }

    /** 비회원 예약 취소 — CANCELED 전이, D-1 이전이면 환불 요청 기록 */
    @DeleteMapping("/{bookingId}")
    public CancelResponse cancelBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-Access-Token") String token) {
        BookingCancelUseCase.CancelResult result =
                bookingCancelUseCase.cancelBooking(bookingId, token);
        return CancelResponse.of(result.booking(), result.refundable());
    }
}
