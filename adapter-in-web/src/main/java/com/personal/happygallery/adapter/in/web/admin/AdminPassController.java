package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.adapter.in.web.admin.dto.BatchResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.PassRefundResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/passes", "/admin/passes"})
public class AdminPassController {

    private final PassExpiryBatchUseCase passExpiryBatchUseCase;
    private final PassRefundUseCase passRefundUseCase;

    public AdminPassController(PassExpiryBatchUseCase passExpiryBatchUseCase,
                               PassRefundUseCase passRefundUseCase) {
        this.passExpiryBatchUseCase = passExpiryBatchUseCase;
        this.passRefundUseCase = passRefundUseCase;
    }

    /** 만료 배치 수동 트리거 — 스케줄러 미구현 시 운영자가 직접 호출 */
    @PostMapping("/expire")
    public BatchResponse triggerExpiry() {
        BatchResult result = passExpiryBatchUseCase.expireAll();
        return BatchResponse.from(result);
    }

    /**
     * 8회권 전체 환불 — 미래 예약 자동 취소 + 잔여 크레딧 소멸.
     * 실제 PG 환불은 refundAmount를 참고해 관리자가 수동 처리.
     */
    @PostMapping("/{passId}/refund")
    public PassRefundResponse refundPass(@PathVariable Long passId) {
        var result = passRefundUseCase.refundPass(passId);
        return PassRefundResponse.from(result);
    }
}
