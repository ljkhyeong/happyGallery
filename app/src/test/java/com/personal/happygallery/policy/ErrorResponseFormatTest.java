package com.personal.happygallery.policy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [PolicyTest] 에러 응답 포맷 고정 검증.
 *
 * <p>에러 응답은 반드시 {@code code}, {@code message} 두 필드만 포함한다.
 * 필드 추가·삭제·이름 변경은 이 테스트를 먼저 수정해야 한다.
 *
 * <pre>
 * { "code": "ALREADY_REFUNDED", "message": "이미 환불된 건입니다." }
 * </pre>
 */
@Tag("policy")
class ErrorResponseFormatTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("에러 응답은 code와 message 두 필드만 직렬화된다")
    @Test
    void errorResponse_serializesExactlyTwoFields() throws Exception {
        ErrorResponse response = ErrorResponse.of(ErrorCode.ALREADY_REFUNDED);

        JsonNode node = objectMapper.valueToTree(response);

        assertThat(node.size()).isEqualTo(2);
        assertThat(node.has("code")).isTrue();
        assertThat(node.has("message")).isTrue();
    }

    @DisplayName("에러 응답의 code 값은 ErrorCode 이름과 일치한다")
    @Test
    void errorResponse_codeEqualsErrorCodeName() throws Exception {
        ErrorResponse response = ErrorResponse.of(ErrorCode.ALREADY_REFUNDED);

        JsonNode node = objectMapper.valueToTree(response);

        assertThat(node.get("code").asText()).isEqualTo("ALREADY_REFUNDED");
        assertThat(node.get("message").asText()).isEqualTo(ErrorCode.ALREADY_REFUNDED.message);
    }

    @DisplayName("에러 응답에 사용자 메시지를 지정하면 기본 메시지를 대체한다")
    @Test
    void errorResponse_customMessage_overridesDefault() throws Exception {
        ErrorResponse response = ErrorResponse.of(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다.");

        JsonNode node = objectMapper.valueToTree(response);

        assertThat(node.get("code").asText()).isEqualTo("NOT_FOUND");
        assertThat(node.get("message").asText()).isEqualTo("주문을 찾을 수 없습니다.");
    }
}
