package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.support.UseCaseIT;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class RequestIdFilterUseCaseIT {

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
        var result = mockMvc.perform(get("/api/v1/classes"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Request-Id"))
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
    }

    @DisplayName("요청 ID를 전달하면 동일한 ID를 반환한다")
    @Test
    void whenRequestIdProvided_returnsSameId() throws Exception {
        mockMvc.perform(get("/api/v1/classes")
                        .header("X-Request-Id", "test-request-id-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Request-Id", "test-request-id-123"));
    }

    @DisplayName("제어 문자가 있거나 64자를 초과한 요청 ID는 새 UUID로 교체한다")
    @ParameterizedTest(name = "[{index}] 안전하지 않은 요청 ID를 교체한다")
    @MethodSource("unsafeRequestIds")
    void whenUnsafeRequestIdProvided_replacesWithUuid(String unsafeRequestId) throws Exception {
        var result = mockMvc.perform(get("/api/v1/classes")
                        .header("X-Request-Id", unsafeRequestId))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = result.getResponse().getHeader("X-Request-Id");
        assertThat(UUID.fromString(requestId).toString()).isEqualTo(requestId);
    }

    private static Stream<String> unsafeRequestIds() {
        return Stream.of(
                "request-id\r\nInjected-Header:value",
                "a".repeat(65));
    }
}
