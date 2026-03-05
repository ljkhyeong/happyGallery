package com.personal.happygallery.app.web;

import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class RequestIdFilterTest {

    @Autowired
    WebApplicationContext context;

    @Autowired
    RequestIdFilter requestIdFilter;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(requestIdFilter)
                .build();
    }

    @DisplayName("요청 ID가 없으면 서버가 요청 ID를 생성해 반환한다")
    @Test
    void whenNoRequestId_generatesAndReturns() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    @DisplayName("요청 ID를 전달하면 동일한 ID를 반환한다")
    @Test
    void whenRequestIdProvided_returnsSameId() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Request-Id", "test-request-id-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-id-123"));
    }
}
