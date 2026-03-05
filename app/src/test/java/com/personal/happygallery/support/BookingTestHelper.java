package com.personal.happygallery.support;

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

    /** 충분히 먼 미래 슬롯 시작 시각 — isRefundable()/isChangeable() 항상 true */
    public static final LocalDateTime FUTURE = LocalDateTime.of(2030, 1, 1, 10, 0);

    private final MockMvc mockMvc;

    public BookingTestHelper(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public String sendVerificationAndGetCode(String phone) throws Exception {
        String resp = mockMvc.perform(post("/bookings/phone-verifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "phone": "%s" }
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(resp, "$.code");
    }

    public String createBooking(String phone, String code, Long slotId, long deposit) throws Exception {
        return mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "홍길동",
                                  "slotId": %d,
                                  "depositAmount": %d,
                                  "paymentMethod": "CARD"
                                }
                                """.formatted(phone, code, slotId, deposit)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    public String bookWithPass(String phone, String code, Long slotId, Long passId) throws Exception {
        return mockMvc.perform(post("/bookings/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "verificationCode": "%s",
                                  "name": "김테스트",
                                  "slotId": %d,
                                  "passId": %d
                                }
                                """.formatted(phone, code, slotId, passId)))
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
