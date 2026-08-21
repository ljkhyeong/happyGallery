package com.personal.happygallery.policy;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * [PolicyTest] 에러 응답 포맷 고정 검증.
 *
 * <p>에러 응답은 {@code code}, {@code message}를 항상 포함하고,
 * 요청 추적 ID가 있으면 {@code requestId}를 추가한다.
 * 필드 추가·삭제·이름 변경은 이 테스트를 먼저 수정해야 한다.
 *
 * <pre>
 * { "code": "ALREADY_REFUNDED", "message": "이미 환불된 건입니다." }
 * </pre>
 */
@Tag("policy")
class ErrorResponseFormatPolicyTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("요청 추적 ID가 없는 에러 응답은 code와 message 값만 직렬화된다")
    @Test
    void errorResponse_serializesExactlyTwoFields() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.ALREADY_REFUNDED);

        JsonNode node = objectMapper.valueToTree(response);

        assertSoftly(softly -> {
            softly.assertThat(node.size()).isEqualTo(2);
            softly.assertThat(node.get("code").asText())
                    .isEqualTo(ErrorCode.ALREADY_REFUNDED.name());
            softly.assertThat(node.get("message").asText())
                    .isEqualTo(ErrorCode.ALREADY_REFUNDED.message);
        });
    }

    @DisplayName("요청 추적 ID가 있는 에러 응답은 requestId를 함께 직렬화한다")
    @Test
    void errorResponse_withRequestId_serializesTracingField() {
        ErrorResponse response = ErrorResponse.of(
                ErrorCode.ALREADY_REFUNDED,
                ErrorCode.ALREADY_REFUNDED.message,
                "request-id");

        JsonNode node = objectMapper.valueToTree(response);

        assertSoftly(softly -> {
            softly.assertThat(node.size()).isEqualTo(3);
            softly.assertThat(node.get("requestId").asText()).isEqualTo("request-id");
        });
    }

    @DisplayName("에러 응답에 사용자 메시지를 지정하면 기본 메시지를 대체한다")
    @Test
    void errorResponse_customMessage_overridesDefault() {
        ErrorResponse response = ErrorResponse.of(ErrorCode.NOT_FOUND, "주문을 찾을 수 없습니다.");

        JsonNode node = objectMapper.valueToTree(response);

        assertSoftly(softly -> {
            softly.assertThat(node.get("code").asText()).isEqualTo(ErrorCode.NOT_FOUND.name());
            softly.assertThat(node.get("message").asText()).isEqualTo("주문을 찾을 수 없습니다.");
        });
    }
}
