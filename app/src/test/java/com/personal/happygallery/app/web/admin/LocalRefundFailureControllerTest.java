package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.payment.DefaultDevRefundFailureService;
import com.personal.happygallery.app.payment.port.in.DevRefundFailureUseCase;
import com.personal.happygallery.app.payment.port.out.RefundFailureScriptPort;
import com.personal.happygallery.app.web.admin.dto.ArmRefundFailureResponse;
import com.personal.happygallery.app.web.admin.dto.FailNextRefundRequest;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class LocalRefundFailureControllerTest {

    @DisplayName("빈 사유로 다음 환불 실패를 arm하면 기본 사유를 사용한다")
    @Test
    void failNext_usesDefaultReason_whenReasonIsBlank() {
        InMemoryRefundFailureScript script = new InMemoryRefundFailureScript();
        DevRefundFailureUseCase useCase = new DefaultDevRefundFailureService(script);
        LocalRefundFailureController controller = new LocalRefundFailureController(useCase);

        ArmRefundFailureResponse response = controller.failNext(new FailNextRefundRequest("   "));

        assertSoftly(softly -> {
            softly.assertThat(response.status()).isEqualTo("ARMED");
            softly.assertThat(response.reason()).isEqualTo(LocalRefundFailureController.DEFAULT_REASON);
            softly.assertThat(script.consumeNextFailureReason())
                    .contains(LocalRefundFailureController.DEFAULT_REASON);
        });
    }

    @DisplayName("다음 환불 실패 arm 해제를 요청하면 대기 중인 실패가 제거된다")
    @Test
    void clear_removesArmedFailure() {
        InMemoryRefundFailureScript script = new InMemoryRefundFailureScript();
        DevRefundFailureUseCase useCase = new DefaultDevRefundFailureService(script);
        LocalRefundFailureController controller = new LocalRefundFailureController(useCase);

        controller.failNext(new FailNextRefundRequest("수동 실패"));
        controller.clear();

        assertThat(script.consumeNextFailureReason()).isEmpty();
    }

    /** 테스트용 인메모리 RefundFailureScript 구현. */
    private static class InMemoryRefundFailureScript implements RefundFailureScriptPort {
        private final AtomicReference<String> nextFailureReason = new AtomicReference<>();

        @Override
        public void armNextFailure(String reason) { nextFailureReason.set(reason); }

        @Override
        public void clear() { nextFailureReason.set(null); }

        public Optional<String> consumeNextFailureReason() {
            return Optional.ofNullable(nextFailureReason.getAndSet(null));
        }
    }
}
