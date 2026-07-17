package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class AdminLoginUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired AdminUserPort adminUserPort;
    @Autowired TestCleanupSupport cleanupSupport;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearAdminUsers();
        adminUserPort.save(new AdminUser(
                "admin",
                new BCryptPasswordEncoder().encode("admin1234")));
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearAdminUsers();
    }

    @DisplayName("관리자 계정으로 로그인할 수 있다")
    @Test
    void login_defaultAdmin_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }
}
