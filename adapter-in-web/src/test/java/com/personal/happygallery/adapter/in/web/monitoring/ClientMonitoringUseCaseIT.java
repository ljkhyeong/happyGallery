package com.personal.happygallery.adapter.in.web.monitoring;

import com.personal.happygallery.application.customer.port.out.PhoneVerificationReaderPort;
import com.personal.happygallery.support.CustomerTestHelper;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class ClientMonitoringUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired PhoneVerificationReaderPort phoneVerificationReader;
    @Autowired ObjectMapper objectMapper;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearUsers();
    }

    @DisplayName("비회원도 client monitoring 이벤트를 전송할 수 있다")
    @Test
    void guest_canSendClientMonitoringEvent() throws Exception {
        mockMvc.perform(post("/api/v1/monitoring/client-events")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "GUEST_LOOKUP_HUB_VIEWED",
                                  "path": "/guest",
                                  "source": "home_lookup_panel",
                                  "target": "hub"
                                }
                                """))
                .andExpect(status().isNoContent());
    }

    @DisplayName("회원 세션이 있어도 client monitoring 이벤트를 전송할 수 있다")
    @Test
    void member_canSendClientMonitoringEvent() throws Exception {
        Cookie sessionCookie = new CustomerTestHelper(mockMvc, objectMapper, phoneVerificationReader)
                .signupAndGetSessionCookie("monitor@example.com", "01012341234");

        mockMvc.perform(post("/api/v1/monitoring/client-events")
                        .with(csrf())
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "event": "GUEST_CLAIM_MODAL_OPENED",
                                  "path": "/my",
                                  "source": "claim_query_auto_open",
                                  "target": "phone_verification"
                                }
                                """))
                .andExpect(status().isNoContent());
    }
}
