package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.booking.RefundRetryService;
import com.personal.happygallery.app.web.admin.dto.FailedRefundResponse;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/refunds", "/admin/refunds"})
public class AdminRefundController {

    private final RefundRetryService refundRetryService;

    public AdminRefundController(RefundRetryService refundRetryService) {
        this.refundRetryService = refundRetryService;
    }

    /** FAILED 환불 목록 조회 */
    @GetMapping("/failed")
    public List<FailedRefundResponse> listFailed() {
        return refundRetryService.listFailed().stream()
                .map(FailedRefundResponse::from)
                .toList();
    }

    /** 특정 환불 재시도 */
    @PostMapping("/{refundId}/retry")
    public ResponseEntity<Void> retry(@PathVariable Long refundId) {
        refundRetryService.retry(refundId);
        return ResponseEntity.ok().build();
    }
}
