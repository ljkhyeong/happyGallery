package com.personal.happygallery.app.web.booking;

import com.personal.happygallery.app.booking.BookingQueryService;
import com.personal.happygallery.app.booking.BookingRescheduleService;
import com.personal.happygallery.app.booking.GuestBookingService;
import com.personal.happygallery.app.web.booking.dto.BookingDetailResponse;
import com.personal.happygallery.app.web.booking.dto.BookingResponse;
import com.personal.happygallery.app.web.booking.dto.CreateGuestBookingRequest;
import com.personal.happygallery.app.web.booking.dto.RescheduleRequest;
import com.personal.happygallery.app.web.booking.dto.RescheduleResponse;
import com.personal.happygallery.app.web.booking.dto.SendVerificationRequest;
import com.personal.happygallery.app.web.booking.dto.SendVerificationResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.PhoneVerification;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bookings")
public class BookingController {

    private final GuestBookingService guestBookingService;
    private final BookingQueryService bookingQueryService;
    private final BookingRescheduleService bookingRescheduleService;

    public BookingController(GuestBookingService guestBookingService,
                             BookingQueryService bookingQueryService,
                             BookingRescheduleService bookingRescheduleService) {
        this.guestBookingService = guestBookingService;
        this.bookingQueryService = bookingQueryService;
        this.bookingRescheduleService = bookingRescheduleService;
    }

    /** 휴대폰 인증 코드 발송 (MVP: 응답에 code 포함) */
    @PostMapping("/phone-verifications")
    public SendVerificationResponse sendVerification(
            @RequestBody @Valid SendVerificationRequest request) {
        PhoneVerification pv = guestBookingService.sendVerificationCode(request.phone());
        return SendVerificationResponse.from(pv);
    }

    /** 게스트 예약 생성 */
    @PostMapping("/guest")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse createGuestBooking(
            @RequestBody @Valid CreateGuestBookingRequest request) {
        Booking booking = guestBookingService.createGuestBooking(
                request.phone(),
                request.verificationCode(),
                request.name(),
                request.slotId(),
                request.depositAmount());
        return BookingResponse.from(booking);
    }

    /** 비회원 예약 조회 — bookingId + access_token 검증 */
    @GetMapping("/{bookingId}")
    public BookingDetailResponse getBooking(
            @PathVariable Long bookingId,
            @RequestParam String token) {
        Booking booking = bookingQueryService.getBookingByToken(bookingId, token);
        return BookingDetailResponse.from(booking);
    }

    /** 비회원 예약 변경 — 슬롯 교체, 이력 누적 */
    @PatchMapping("/{bookingId}/reschedule")
    public RescheduleResponse reschedule(
            @PathVariable Long bookingId,
            @RequestBody @Valid RescheduleRequest request) {
        Booking booking = bookingRescheduleService.rescheduleBooking(
                bookingId, request.token(), request.newSlotId());
        return RescheduleResponse.from(booking);
    }
}
