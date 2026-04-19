package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.payment.port.in.DevRefundFailureUseCase;
import com.personal.happygallery.adapter.in.web.admin.dto.ArmRefundFailureResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.FailNextRefundRequest;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * local smoke/E2E에서 환불 실패를 재현하기 위한 운영자용 dev 훅.
 */
@Profile("local")
@RestController
@RequestMapping({"/api/v1/admin/dev/payment/refunds", "/admin/dev/payment/refunds"})
public class LocalRefundFailureController {

    static final String DEFAULT_REASON = "로컬 smoke 강제 환불 실패";

    private final DevRefundFailureUseCase devRefundFailure;

    public LocalRefundFailureController(DevRefundFailureUseCase devRefundFailure) {
        this.devRefundFailure = devRefundFailure;
    }

    @PostMapping("/fail-next")
    public ArmRefundFailureResponse failNext(@RequestBody(required = false) @Valid FailNextRefundRequest request) {
        String reason = DEFAULT_REASON;
        if (request != null && StringUtils.hasText(request.reason())) {
            reason = request.reason().trim();
        }

        devRefundFailure.armNextFailure(reason);
        return new ArmRefundFailureResponse("ARMED", reason);
    }

    @DeleteMapping("/fail-next")
    public ResponseEntity<Void> clear() {
        devRefundFailure.clear();
        return ResponseEntity.noContent().build();
    }

}
