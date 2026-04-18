package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.payment.port.in.RefundRetryUseCase;
import com.personal.happygallery.adapter.in.web.admin.dto.FailedRefundResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/refunds", "/admin/refunds"})
public class AdminRefundController {

    private final RefundRetryUseCase refundRetryUseCase;

    public AdminRefundController(RefundRetryUseCase refundRetryUseCase) {
        this.refundRetryUseCase = refundRetryUseCase;
    }

    /** FAILED 환불 목록 조회 */
    @GetMapping("/failed")
    public List<FailedRefundResponse> listFailed() {
        return refundRetryUseCase.listFailed().stream()
                .map(FailedRefundResponse::from)
                .toList();
    }

    /** 특정 환불 재시도 */
    @PostMapping("/{refundId}/retry")
    public void retry(@PathVariable Long refundId) {
        refundRetryUseCase.retry(refundId);
    }
}
