package com.personal.happygallery.app.web.booking;

import com.personal.happygallery.app.booking.BookingQueryService;
import com.personal.happygallery.app.booking.GuestBookingService;
import com.personal.happygallery.app.web.booking.dto.BookingDetailResponse;
import com.personal.happygallery.app.web.booking.dto.BookingResponse;
import com.personal.happygallery.app.web.booking.dto.CreateGuestBookingRequest;
import com.personal.happygallery.app.web.booking.dto.SendVerificationRequest;
import com.personal.happygallery.app.web.booking.dto.SendVerificationResponse;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.PhoneVerification;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
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

    public BookingController(GuestBookingService guestBookingService,
                             BookingQueryService bookingQueryService) {
        this.guestBookingService = guestBookingService;
        this.bookingQueryService = bookingQueryService;
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
}
