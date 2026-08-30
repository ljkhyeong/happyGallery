package com.personal.happygallery.bootstrap;

import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.core.StreamReadConstraints;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class JsonReadConstraintsUseCaseIT {

    @Autowired JsonMapper jsonMapper;
    @Autowired MockMvc mockMvc;

    @DisplayName("공개 JSON 파서는 문서와 토큰과 문자열 크기 상한을 적용한다")
    @Test
    void jsonParser_appliesGlobalReadConstraints() {
        StreamReadConstraints constraints = jsonMapper.tokenStreamFactory().streamReadConstraints();

        assertSoftly(softly -> {
            softly.assertThat(constraints.getMaxDocumentLength()).isEqualTo(2_097_152L);
            softly.assertThat(constraints.getMaxTokenCount()).isEqualTo(50_000L);
            softly.assertThat(constraints.getMaxStringLength()).isEqualTo(1_048_576);
        });
    }

    @DisplayName("문자열 상한을 넘는 JSON 요청은 400으로 거절한다")
    @Test
    void jsonParser_rejectsOversizedStringAtHttpBoundary() throws Exception {
        String oversized = "a".repeat(1_048_577);

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + oversized + "\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 JSON 형식이 올바르지 않습니다."));
    }

    @DisplayName("토큰 상한을 넘는 JSON 요청은 400으로 거절한다")
    @Test
    void jsonParser_rejectsTooManyTokensAtHttpBoundary() throws Exception {
        String items = "{},".repeat(25_001) + "{}";

        mockMvc.perform(post("/api/v1/me/cart/merge")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idempotencyKey\":\"00000000-0000-0000-0000-000000000001\","
                                + "\"items\":[" + items + "]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 JSON 형식이 올바르지 않습니다."));
    }

    @DisplayName("문서 상한을 넘는 JSON 요청은 400으로 거절한다")
    @Test
    void jsonParser_rejectsOversizedDocumentAtHttpBoundary() throws Exception {
        String withinStringLimit = "a".repeat(1_048_570);

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + withinStringLimit
                                + "\",\"password\":\"" + withinStringLimit + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.message").value("요청 JSON 형식이 올바르지 않습니다."));
    }
}
