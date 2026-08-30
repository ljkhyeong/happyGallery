package com.personal.happygallery.support;

import com.personal.happygallery.adapter.in.web.booking.dto.SendVerificationRequest;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.booking.PhoneVerificationPurpose;
import java.time.LocalDateTime;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예약 관련 통합 테스트 공통 헬퍼.
 *
 * <p>전화번호 인증 → 예약 생성 → 응답 추출 패턴의 중복을 제거한다.
 */
public final class BookingTestHelper {

    public record CreatedBooking(Long bookingId, String accessToken) {}

    /** 충분히 먼 미래 슬롯 시작 시각 — isRefundable()/isChangeable() 항상 true */
    public static final LocalDateTime FUTURE = LocalDateTime.of(2030, 1, 1, 10, 0);

    private final MockMvc mockMvc;
    private final PhoneVerificationReaderPort phoneVerificationReaderPort;
    private final ObjectMapper objectMapper;
    private final PaymentTestHelper paymentTestHelper;

    public BookingTestHelper(MockMvc mockMvc,
                             PhoneVerificationReaderPort phoneVerificationReaderPort,
                             ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.phoneVerificationReaderPort = phoneVerificationReaderPort;
        this.objectMapper = objectMapper;
        this.paymentTestHelper = new PaymentTestHelper(mockMvc, objectMapper);
    }

    public String sendVerificationAndGetCode(String phone) throws Exception {
        return sendVerificationAndGetCode(phone, PhoneVerificationPurpose.GUEST_BOOKING);
    }

    public String sendVerificationAndGetCode(
            String phone,
            PhoneVerificationPurpose purpose) throws Exception {
        mockMvc.perform(post("/api/v1/bookings/phone-verifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendVerificationRequest(
                                phone, purpose))))
                .andExpect(status().isOk());
        return phoneVerificationReaderPort.findLatestUnverifiedCode(phone, purpose)
                .orElseThrow(() -> new AssertionError("No verification code found for " + phone))
                .getCode();
    }

    public CreatedBooking createVerifiedCardBooking(String phone, Long slotId) throws Exception {
        return createVerifiedCardBooking(phone, slotId, 1);
    }

    public CreatedBooking createVerifiedCardBooking(
            String phone, Long slotId, int participantCount) throws Exception {
        String code = sendVerificationAndGetCode(phone);
        PaymentTestHelper.ConfirmedPayment confirmed =
                paymentTestHelper.createGuestBooking(
                        phone, code, "홍길동", slotId, participantCount);
        return new CreatedBooking(confirmed.domainId(), confirmed.accessToken());
    }
}
