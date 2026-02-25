package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.booking.RefundRetryService;
import com.personal.happygallery.domain.booking.Refund;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/refunds")
public class AdminRefundController {

    private final RefundRetryService refundRetryService;

    public AdminRefundController(RefundRetryService refundRetryService) {
        this.refundRetryService = refundRetryService;
    }

    /** FAILED 환불 목록 조회 */
    @GetMapping("/failed")
    public List<Map<String, Object>> listFailed() {
        return refundRetryService.listFailed().stream()
                .map(r -> Map.<String, Object>of(
                        "refundId", r.getId(),
                        "bookingId", r.getBooking().getId(),
                        "amount", r.getAmount(),
                        "failReason", r.getFailReason() != null ? r.getFailReason() : "",
                        "createdAt", r.getCreatedAt()))
                .toList();
    }

    /** 특정 환불 재시도 */
    @PostMapping("/{refundId}/retry")
    public ResponseEntity<Void> retry(@PathVariable Long refundId) {
        refundRetryService.retry(refundId);
        return ResponseEntity.ok().build();
    }
}
