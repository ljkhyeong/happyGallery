package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.batch.BatchResult;
import com.personal.happygallery.app.pass.PassExpiryBatchService;
import com.personal.happygallery.app.pass.PassRefundService;
import com.personal.happygallery.app.web.admin.dto.BatchResponse;
import com.personal.happygallery.app.web.admin.dto.PassRefundResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/passes", "/admin/passes"})
public class AdminPassController {

    private final PassExpiryBatchService passExpiryBatchService;
    private final PassRefundService passRefundService;

    public AdminPassController(PassExpiryBatchService passExpiryBatchService,
                               PassRefundService passRefundService) {
        this.passExpiryBatchService = passExpiryBatchService;
        this.passRefundService = passRefundService;
    }

    /** 만료 배치 수동 트리거 — 스케줄러 미구현 시 운영자가 직접 호출 */
    @PostMapping("/expire")
    public BatchResponse triggerExpiry() {
        BatchResult result = passExpiryBatchService.expireAll();
        return BatchResponse.from(result);
    }

    /**
     * 8회권 전체 환불 — 미래 예약 자동 취소 + 잔여 크레딧 소멸.
     * 실제 PG 환불은 refundAmount를 참고해 관리자가 수동 처리.
     */
    @PostMapping("/{passId}/refund")
    public PassRefundResponse refundPass(@PathVariable Long passId) {
        return PassRefundResponse.from(passRefundService.refundPass(passId));
    }
}
