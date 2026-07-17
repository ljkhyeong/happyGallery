package com.personal.happygallery.support;

import com.personal.happygallery.adapter.in.web.customer.dto.SignupRequest;
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

    public CustomerTestHelper(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public Cookie signupAndGetSessionCookie(String email, String phone) throws Exception {
        var result = mockMvc.perform(post("/api/v1/auth/signup")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SignupRequest(email, "password123", "회원", phone))))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie cookie = result.getResponse().getCookie("HG_SESSION");
        assertThat(cookie).isNotNull();
        return cookie;
    }
}
