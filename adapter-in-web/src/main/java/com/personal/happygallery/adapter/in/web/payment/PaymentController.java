package com.personal.happygallery.adapter.in.web.payment;

import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentResponse;
import com.personal.happygallery.adapter.in.web.payment.dto.PreparePaymentRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.PreparePaymentResponse;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareResult;
import com.personal.happygallery.domain.error.PhoneVerificationRequiredException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 prepare / confirm 단일 진입점.
 *
 * <p>주문/예약/8회권 모두 이 컨트롤러를 통해 결제를 시작한다.
 * 회원/비회원 구분은 Spring Security가 주입하는 nullable {@link CustomerPrincipal}로 결정된다.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private static final String PAYMENT_STATUS_TOKEN_HEADER = "X-Payment-Status-Token";

    private final PaymentPrepareUseCase prepareUseCase;
    private final PaymentConfirmUseCase confirmUseCase;
    private final SubjectRateLimitGuard rateLimitGuard;

    public PaymentController(PaymentPrepareUseCase prepareUseCase,
                             PaymentConfirmUseCase confirmUseCase,
                             SubjectRateLimitGuard rateLimitGuard) {
        this.prepareUseCase = prepareUseCase;
        this.confirmUseCase = confirmUseCase;
        this.rateLimitGuard = rateLimitGuard;
    }

    @PostMapping("/prepare")
    @Operation(operationId = "preparePayment")
    public PreparePaymentResponse prepare(@RequestBody @Valid PreparePaymentRequest req,
                                          @AuthenticationPrincipal CustomerPrincipal customer,
                                          HttpServletResponse response) {
        if (customer != null && (!customer.phoneVerified() || customer.phone() == null)) {
            throw new PhoneVerificationRequiredException();
        }
        AuthContext auth = customer != null
                ? AuthContext.member(customer.userId())
                : AuthContext.guest();
        PrepareResult result = prepareUseCase.prepare(new PrepareCommand(req.context(), req.payload(), auth));
        setNoStore(response);
        return PreparePaymentResponse.from(result);
    }

    @PostMapping("/confirm")
    @Operation(operationId = "confirmPayment")
    public ConfirmPaymentResponse confirm(@RequestBody @Valid ConfirmPaymentRequest req,
                                          @RequestHeader(value = PAYMENT_STATUS_TOKEN_HEADER, required = false)
                                          String statusToken,
                                          @AuthenticationPrincipal CustomerPrincipal customer,
                                          HttpServletResponse response) {
        rateLimitGuard.checkPaymentConfirm(req.orderId());
        AuthContext auth = customer != null
                ? AuthContext.member(customer.userId())
                : AuthContext.guest();
        ConfirmResult result = confirmUseCase.confirm(
                ConfirmCommand.customerRequest(
                        req.paymentKey(), req.orderId(), req.amount(), auth, statusToken));
        setNoStore(response);
        return ConfirmPaymentResponse.from(result);
    }

    private void setNoStore(HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
    }
}
