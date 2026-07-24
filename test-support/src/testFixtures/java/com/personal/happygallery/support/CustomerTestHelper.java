package com.personal.happygallery.support;

import com.personal.happygallery.adapter.in.web.booking.dto.SendVerificationRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.SignupRequest;
import com.personal.happygallery.adapter.in.web.policy.dto.PolicyAcceptanceRequest;
import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.domain.user.KoreanPhoneNumber;
import jakarta.servlet.http.Cookie;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 회원 인증이 사전 조건인 통합 테스트용 요청 헬퍼. */
public final class CustomerTestHelper {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final PhoneVerificationReaderPort phoneVerificationReader;

    public CustomerTestHelper(MockMvc mockMvc,
                              ObjectMapper objectMapper,
                              PhoneVerificationReaderPort phoneVerificationReader) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.phoneVerificationReader = phoneVerificationReader;
    }

    public Cookie signupAndGetSessionCookie(String email, String phone) throws Exception {
        String normalizedPhone = KoreanPhoneNumber.required(phone);
        mockMvc.perform(post("/api/v1/bookings/phone-verifications")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendVerificationRequest(normalizedPhone))))
                .andExpect(status().isOk());
        String verificationCode = phoneVerificationReader.findLatestUnverifiedCode(normalizedPhone)
                .orElseThrow(() -> new AssertionError("No verification code found"))
                .getCode();
        var result = mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(
                                        email,
                                        "password123",
                                        "회원",
                                        phone,
                                        verificationCode,
                                        new PolicyAcceptanceRequest(
                                                "2026-07-21-v1",
                                                true,
                                                "2026-07-21-v1",
                                                true)))))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("HG_SESSION");
        assertThat(cookie).isNotNull();
        return cookie;
    }
}
