package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class CustomerAuthUseCaseIT {

    @Autowired
    MockMvc mockMvc;

    @DisplayName("회원가입 후 사용자 정보와 세션 쿠키를 받는다")
    @Test
    void signup_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "test@example.com",
                                  "password": "password123",
                                  "name": "테스트",
                                  "phone": "010-1234-5678"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.name").value("테스트"))
                .andExpect(jsonPath("$.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.phoneVerified").value(false))
                .andExpect(cookie().exists("HG_SESSION"))
                .andExpect(cookie().httpOnly("HG_SESSION", true));
    }

    @DisplayName("중복 이메일로 회원가입하면 409를 반환한다")
    @Test
    void signup_duplicateEmail_conflict() throws Exception {
        String body = """
                {
                  "email": "dup@example.com",
                  "password": "password123",
                  "name": "테스트",
                  "phone": "010-0000-0000"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @DisplayName("로그인 성공 시 사용자 정보와 세션 쿠키를 받는다")
    @Test
    void login_success() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123",
                                  "name": "로그인",
                                  "phone": "010-1111-2222"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "login@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"))
                .andExpect(cookie().exists("HG_SESSION"));
    }

    @DisplayName("잘못된 비밀번호로 로그인하면 401을 반환한다")
    @Test
    void login_wrongPassword_unauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong@example.com",
                                  "password": "password123",
                                  "name": "테스트",
                                  "phone": "010-3333-4444"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong@example.com",
                                  "password": "wrongpassword"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @DisplayName("로그아웃 후 세션 쿠키가 삭제된다")
    @Test
    void logout_clearsCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("HG_SESSION", 0));
    }
}
