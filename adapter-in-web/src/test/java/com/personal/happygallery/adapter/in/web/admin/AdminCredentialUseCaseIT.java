package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminPasswordChangeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.LoginRequest;
import com.personal.happygallery.application.admin.port.out.AdminUserPort;
import com.personal.happygallery.domain.admin.AdminUser;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class AdminCredentialUseCaseIT {

    private static final String OLD_PASSWORD = "admin123456";
    private static final String NEW_PASSWORD = "new-admin-123456";

    @Autowired WebApplicationContext context;
    @Autowired ObjectMapper objectMapper;
    @Autowired AdminUserPort adminUserPort;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired TestCleanupSupport cleanupSupport;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        cleanupSupport.clearAdminUsers();
        adminUserPort.save(new AdminUser("admin", passwordEncoder.encode(OLD_PASSWORD)));
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearAdminUsers();
    }

    @DisplayName("관리자 비밀번호를 변경하면 기존 세션을 모두 폐기하고 새 비밀번호만 허용한다")
    @Test
    void changePassword_revokesAllExistingSessions() throws Exception {
        String firstToken = loginAndGetToken(OLD_PASSWORD);
        String secondToken = loginAndGetToken(OLD_PASSWORD);

        mockMvc.perform(patch("/api/v1/admin/auth/password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new AdminPasswordChangeRequest(OLD_PASSWORD, NEW_PASSWORD))))
                .andExpect(status().isNoContent());

        assertRejected(firstToken);
        assertRejected(secondToken);
        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest("admin", OLD_PASSWORD))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        loginAndGetToken(NEW_PASSWORD);
    }

    private String loginAndGetToken(String password) throws Exception {
        var result = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("token")
                .asText();
    }

    private void assertRejected(String token) throws Exception {
        mockMvc.perform(get("/api/v1/admin/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
