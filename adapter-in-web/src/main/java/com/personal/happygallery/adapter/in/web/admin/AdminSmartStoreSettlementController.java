package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreSettlementIssueResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreSettlementSyncResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SynchronizeSmartStoreSettlementRequest;
import com.personal.happygallery.application.order.port.in.SmartStoreSettlementUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/smartstore-settlements")
public class AdminSmartStoreSettlementController {

    private static final int ISSUE_LIMIT = 100;

    private final SmartStoreSettlementUseCase useCase;

    public AdminSmartStoreSettlementController(SmartStoreSettlementUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/issues")
    @Operation(
            operationId = "listSmartStoreSettlementIssues",
            summary = "스마트스토어 정산 대사 불일치 목록 조회")
    public List<SmartStoreSettlementIssueResponse> listIssues() {
        return useCase.findIssues(ISSUE_LIMIT).stream()
                .map(SmartStoreSettlementIssueResponse::from)
                .toList();
    }

    @PostMapping("/synchronize")
    @Operation(
            operationId = "synchronizeSmartStoreSettlements",
            summary = "스마트스토어 정산 기간 재동기화")
    public SmartStoreSettlementSyncResponse synchronize(
            @Valid @RequestBody SynchronizeSmartStoreSettlementRequest request) {
        return SmartStoreSettlementSyncResponse.from(
                useCase.synchronize(request.from(), request.to()));
    }
}
