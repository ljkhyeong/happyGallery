package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminPassResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.BatchResponse;
import com.personal.happygallery.adapter.in.web.payment.dto.PassRefundResponse;
import com.personal.happygallery.application.batch.BatchResult;
import com.personal.happygallery.application.pass.port.in.PassExpiryBatchUseCase;
import com.personal.happygallery.application.pass.port.in.PassRefundUseCase;
import com.personal.happygallery.application.search.port.in.AdminPassQueryUseCase;
import com.personal.happygallery.application.shared.page.OffsetPage;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/passes")
public class AdminPassController {

    private final PassExpiryBatchUseCase passExpiryBatchUseCase;
    private final PassRefundUseCase passRefundUseCase;
    private final AdminPassQueryUseCase adminPassQueryUseCase;

    public AdminPassController(PassExpiryBatchUseCase passExpiryBatchUseCase,
                               PassRefundUseCase passRefundUseCase,
                               AdminPassQueryUseCase adminPassQueryUseCase) {
        this.passExpiryBatchUseCase = passExpiryBatchUseCase;
        this.passRefundUseCase = passRefundUseCase;
        this.adminPassQueryUseCase = adminPassQueryUseCase;
    }

    @GetMapping("/search")
    @Operation(operationId = "searchAdminPasses")
    public OffsetPage<AdminPassResponse> searchPasses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return AdminPassResponse.fromPage(adminPassQueryUseCase.search(keyword, page, size));
    }

    @GetMapping("/{passId}")
    @Operation(operationId = "getAdminPass")
    public AdminPassResponse getPass(@PathVariable Long passId) {
        return AdminPassResponse.from(adminPassQueryUseCase.get(passId));
    }

    /** 만료 배치 수동 트리거. 정기 스케줄 외에 운영자가 즉시 실행할 때 사용한다. */
    @PostMapping("/expire")
    public BatchResponse triggerExpiry() {
        BatchResult result = passExpiryBatchUseCase.expireAll();
        return BatchResponse.from(result);
    }

    /**
     * 8회권 전체 환불 — 미래 예약 자동 취소 + 잔여 크레딧 소멸.
     * PG 환불 이력은 성공/실패 상태로 응답하고, 실패 건은 재시도 대상이 된다.
     */
    @PostMapping("/{passId}/refund")
    public PassRefundResponse refundPass(@PathVariable Long passId) {
        var result = passRefundUseCase.refundPass(passId);
        return PassRefundResponse.from(result);
    }
}
