package com.personal.happygallery.support;

import com.personal.happygallery.app.customer.port.out.PhoneVerificationReaderPort;
import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예약 관련 통합 테스트 공통 헬퍼.
 *
 * <p>전화번호 인증 → 예약 생성 → 응답 추출 패턴의 중복을 제거한다.
 */
public final class BookingTestHelper {

    public record CreatedBooking(Long bookingId, String accessToken, String responseBody) {}

    /** 충분히 먼 미래 슬롯 시작 시각 — isRefundable()/isChangeable() 항상 true */
    public static final LocalDateTime FUTURE = LocalDateTime.of(2030, 1, 1, 10, 0);

    private final MockMvc mockMvc;
    private final PhoneVerificationReaderPort phoneVerificationReaderPort;

    public BookingTestHelper(MockMvc mockMvc, PhoneVerificationReaderPort phoneVerificationReaderPort) {
        this.mockMvc = mockMvc;
        this.phoneVerificationReaderPort = phoneVerificationReaderPort;
    }

    public String sendVerificationAndGetCode(String phone) throws Exception {
        mockMvc.perform(post("/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "%s" }
                                """.formatted(phone)))
                .andExpect(status().isOk());
        return phoneVerificationReaderPort.findLatestUnverifiedCode(phone)
                .orElseThrow(() -> new AssertionError("No verification code found for " + phone))
                .getCode();
    }

    public String createBooking(String phone, String code, Long slotId, long deposit) throws Exception {
        return postGuestBooking("""
                {
                  "phone": "%s",
                  "verificationCode": "%s",
                  "name": "홍길동",
                  "slotId": %d,
                  "depositAmount": %d,
                  "paymentMethod": "CARD"
                }
                """.formatted(phone, code, slotId, deposit));
    }

    public CreatedBooking createVerifiedCardBooking(String phone, Long slotId, long deposit) throws Exception {
        return createVerifiedCardBooking(phone, "홍길동", slotId, deposit);
    }

    public CreatedBooking createVerifiedCardBooking(String phone, String name, Long slotId, long deposit) throws Exception {
        String code = sendVerificationAndGetCode(phone);
        String response = postGuestBooking("""
                {
                  "phone": "%s",
                  "verificationCode": "%s",
                  "name": "%s",
                  "slotId": %d,
                  "depositAmount": %d,
                  "paymentMethod": "CARD"
                }
                """.formatted(phone, code, name, slotId, deposit));
        return new CreatedBooking(extractBookingId(response), extractAccessToken(response), response);
    }

    private String postGuestBooking(String requestBody) throws Exception {
        return mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    public static Long extractBookingId(String json) {
        return ((Number) JsonPath.read(json, "$.bookingId")).longValue();
    }

    public static String extractAccessToken(String json) {
        return JsonPath.read(json, "$.accessToken");
    }
}
