package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.infra.payment.LocalRefundFailureScript;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalRefundFailureControllerTest {

    @DisplayName("빈 사유로 다음 환불 실패를 arm하면 기본 사유를 사용한다")
    @Test
    void failNext_usesDefaultReason_whenReasonIsBlank() {
        LocalRefundFailureScript localRefundFailureScript = new LocalRefundFailureScript();
        LocalRefundFailureController controller = new LocalRefundFailureController(localRefundFailureScript);

        var response = controller.failNext(new LocalRefundFailureController.FailNextRefundRequest("   ", null));

        assertThat(response).containsEntry("status", "ARMED");
        assertThat(response).containsEntry("reason", LocalRefundFailureController.DEFAULT_REASON);
        assertThat(localRefundFailureScript.consumeIfMatches(null))
                .contains(LocalRefundFailureController.DEFAULT_REASON);
    }

    @DisplayName("다음 환불 실패 arm 해제를 요청하면 대기 중인 실패가 제거된다")
    @Test
    void clear_removesArmedFailure() {
        LocalRefundFailureScript localRefundFailureScript = new LocalRefundFailureScript();
        LocalRefundFailureController controller = new LocalRefundFailureController(localRefundFailureScript);

        controller.failNext(new LocalRefundFailureController.FailNextRefundRequest("수동 실패", null));
        controller.clear();

        assertThat(localRefundFailureScript.consumeIfMatches(null)).isEmpty();
    }
}
