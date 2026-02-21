package com.personal.happygallery.app.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
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

    @Test
    void whenNoRequestId_generatesAndReturns() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"));
    }

    @Test
    void whenRequestIdProvided_returnsSameId() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header("X-Request-Id", "test-request-id-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-id-123"));
    }
}
