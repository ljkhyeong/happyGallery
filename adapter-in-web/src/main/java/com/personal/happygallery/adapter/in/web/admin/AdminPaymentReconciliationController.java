package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.PaymentReconciliationRequiredResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.PaymentReconciliationResultResponse;
import com.personal.happygallery.application.payment.port.in.PaymentReconciliationAdminUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payment-attempts")
public class AdminPaymentReconciliationController {

    private final PaymentReconciliationAdminUseCase reconciliationAdminUseCase;

    public AdminPaymentReconciliationController(
            PaymentReconciliationAdminUseCase reconciliationAdminUseCase) {
        this.reconciliationAdminUseCase = reconciliationAdminUseCase;
    }

    @GetMapping("/reconciliation-required")
    public List<PaymentReconciliationRequiredResponse> listRequired() {
        return reconciliationAdminUseCase.listRequired().stream()
                .map(PaymentReconciliationRequiredResponse::from)
                .toList();
    }

    @PostMapping("/{attemptId}/reconcile")
    public PaymentReconciliationResultResponse reconcile(@PathVariable Long attemptId) {
        return PaymentReconciliationResultResponse.from(reconciliationAdminUseCase.reconcile(attemptId));
    }
}
