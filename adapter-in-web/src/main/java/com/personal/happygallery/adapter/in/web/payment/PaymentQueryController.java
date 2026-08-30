package com.personal.happygallery.adapter.in.web.payment;

import com.personal.happygallery.adapter.in.web.payment.dto.PassPaymentPolicyResponse;
import com.personal.happygallery.adapter.in.web.payment.dto.PaymentStatusResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.pass.PassPriceProperties;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentStatusQueryUseCase;
import com.personal.happygallery.domain.pass.PassPurchase;
import com.personal.happygallery.domain.time.TimeBoundary;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentQueryController {

    private static final String PAYMENT_STATUS_TOKEN_HEADER = "X-Payment-Status-Token";

    private final PaymentStatusQueryUseCase statusQueryUseCase;
    private final PassPriceProperties passPriceProperties;

    public PaymentQueryController(PaymentStatusQueryUseCase statusQueryUseCase,
                                  PassPriceProperties passPriceProperties) {
        this.statusQueryUseCase = statusQueryUseCase;
        this.passPriceProperties = passPriceProperties;
    }

    @Operation(operationId = "getPaymentStatus")
    @GetMapping("/{orderId}")
    public PaymentStatusResponse getStatus(
            @PathVariable String orderId,
            @RequestHeader(value = PAYMENT_STATUS_TOKEN_HEADER, required = false) String statusToken,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        AuthContext auth = customer == null
                ? AuthContext.guest()
                : AuthContext.member(customer.userId());
        return PaymentStatusResponse.from(statusQueryUseCase.getStatus(orderId, auth, statusToken));
    }

    @Operation(operationId = "getPassPaymentPolicy")
    @GetMapping("/pass-policy")
    public PassPaymentPolicyResponse getPassPolicy() {
        return new PassPaymentPolicyResponse(
                passPriceProperties.totalPrice(),
                PassPurchase.TOTAL_CREDITS,
                TimeBoundary.PASS_VALIDITY_DAYS);
    }
}
