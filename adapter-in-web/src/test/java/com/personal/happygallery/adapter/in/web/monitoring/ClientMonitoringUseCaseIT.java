package com.personal.happygallery.adapter.in.web.monitoring;

import com.personal.happygallery.adapter.in.web.CustomerAuthFilter;
import com.personal.happygallery.support.UseCaseIT;
import jakarta.servlet.Filter;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class ClientMonitoringUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired CustomerAuthFilter customerAuthFilter;
    @Autowired @Qualifier("springSessionRepositoryFilter") Filter springSessionRepositoryFilter;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSessionRepositoryFilter, customerAuthFilter)
                .build();
    }

    @DisplayName("비회원도 client monitoring 이벤트를 전송할 수 있다")
    @Test
    void guest_canSendClientMonitoringEvent() throws Exception {
        mockMvc.perform(post("/api/v1/monitoring/client-events")
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
        MvcResult signup = mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "monitor@example.com",
                                  "password": "password123",
                                  "name": "모니터",
                                  "phone": "01012341234"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie sessionCookie = signup.getResponse().getCookie("HG_SESSION");

        mockMvc.perform(post("/api/v1/monitoring/client-events")
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
