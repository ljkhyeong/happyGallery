package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.FailedRefundResponse;
import com.personal.happygallery.adapter.in.web.payment.dto.RefundStatusResponse;
import com.personal.happygallery.application.payment.port.in.RefundQueryUseCase;
import com.personal.happygallery.application.payment.port.in.RefundRetryUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/admin/refunds")
public class AdminRefundController {

    private final RefundRetryUseCase refundRetryUseCase;
    private final RefundQueryUseCase refundQueryUseCase;

    public AdminRefundController(RefundRetryUseCase refundRetryUseCase,
                                 RefundQueryUseCase refundQueryUseCase) {
        this.refundRetryUseCase = refundRetryUseCase;
        this.refundQueryUseCase = refundQueryUseCase;
    }

    /** 실패·재시도 대기·상태 확인 필요 환불 목록 조회 */
    @GetMapping("/failed")
    public CursorPage<FailedRefundResponse> listFailed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        var page = refundRetryUseCase.listFailed(cursor, size);
        return new CursorPage<>(
                page.content().stream().map(FailedRefundResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }

    @GetMapping("/{refundId}")
    public RefundStatusResponse getRefund(@PathVariable Long refundId) {
        return RefundStatusResponse.from(refundQueryUseCase.getRefund(refundId));
    }

    /** 특정 환불 재시도 */
    @PostMapping("/{refundId}/retry")
    public RefundStatusResponse retry(@PathVariable Long refundId) {
        return RefundStatusResponse.from(refundRetryUseCase.retry(refundId));
    }
}
