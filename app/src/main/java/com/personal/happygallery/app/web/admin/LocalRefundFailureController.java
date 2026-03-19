package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.infra.payment.LocalRefundFailureScript;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.Map;
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

    private final LocalRefundFailureScript localRefundFailureScript;

    public LocalRefundFailureController(LocalRefundFailureScript localRefundFailureScript) {
        this.localRefundFailureScript = localRefundFailureScript;
    }

    @PostMapping("/fail-next")
    public Map<String, String> failNext(@RequestBody(required = false) @Valid FailNextRefundRequest request) {
        String reason = DEFAULT_REASON;
        if (request != null && StringUtils.hasText(request.reason())) {
            reason = request.reason().trim();
        }

        localRefundFailureScript.armNextFailure(reason);
        return Map.of("status", "ARMED", "reason", reason);
    }

    @DeleteMapping("/fail-next")
    public ResponseEntity<Void> clear() {
        localRefundFailureScript.clear();
        return ResponseEntity.noContent().build();
    }

    public record FailNextRefundRequest(@Size(max = 120) String reason) {}
}
