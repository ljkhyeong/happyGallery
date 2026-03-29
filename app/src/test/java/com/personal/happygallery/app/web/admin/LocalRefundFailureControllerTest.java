package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.web.admin.dto.ArmRefundFailureResponse;
import com.personal.happygallery.app.web.admin.dto.FailNextRefundRequest;
import com.personal.happygallery.infra.payment.LocalRefundFailureScript;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class LocalRefundFailureControllerTest {

    @DisplayName("빈 사유로 다음 환불 실패를 arm하면 기본 사유를 사용한다")
    @Test
    void failNext_usesDefaultReason_whenReasonIsBlank() {
        LocalRefundFailureScript localRefundFailureScript = new LocalRefundFailureScript();
        LocalRefundFailureController controller = new LocalRefundFailureController(localRefundFailureScript);

        ArmRefundFailureResponse response = controller.failNext(new FailNextRefundRequest("   "));

        assertSoftly(softly -> {
            softly.assertThat(response.status()).isEqualTo("ARMED");
            softly.assertThat(response.reason()).isEqualTo(LocalRefundFailureController.DEFAULT_REASON);
            softly.assertThat(localRefundFailureScript.consumeNextFailureReason())
                    .contains(LocalRefundFailureController.DEFAULT_REASON);
        });
    }

    @DisplayName("다음 환불 실패 arm 해제를 요청하면 대기 중인 실패가 제거된다")
    @Test
    void clear_removesArmedFailure() {
        LocalRefundFailureScript localRefundFailureScript = new LocalRefundFailureScript();
        LocalRefundFailureController controller = new LocalRefundFailureController(localRefundFailureScript);

        controller.failNext(new FailNextRefundRequest("수동 실패"));
        controller.clear();

        assertThat(localRefundFailureScript.consumeNextFailureReason()).isEmpty();
    }
}
