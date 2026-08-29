package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.PaymentSettlementIssueResponse;
import com.personal.happygallery.application.payment.port.in.PaymentSettlementAdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payment-settlements")
public class AdminPaymentSettlementController {

    private static final int ISSUE_LIMIT = 100;

    private final PaymentSettlementAdminUseCase useCase;

    public AdminPaymentSettlementController(PaymentSettlementAdminUseCase useCase) {
        this.useCase = useCase;
    }

    @Operation(
            operationId = "listPaymentSettlementIssues",
            summary = "PG 정산 대사 불일치 목록 조회")
    @GetMapping("/issues")
    public List<PaymentSettlementIssueResponse> listIssues() {
        return useCase.findIssues(ISSUE_LIMIT).stream()
                .map(PaymentSettlementIssueResponse::from)
                .toList();
    }
}
