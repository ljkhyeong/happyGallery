package com.personal.happygallery.adapter.in.web.payment;

import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.ConfirmPaymentResponse;
import com.personal.happygallery.adapter.in.web.payment.dto.PreparePaymentRequest;
import com.personal.happygallery.adapter.in.web.payment.dto.PreparePaymentResponse;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import com.personal.happygallery.application.payment.port.in.AuthContext;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmCommand;
import com.personal.happygallery.application.payment.port.in.PaymentConfirmUseCase.ConfirmResult;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareCommand;
import com.personal.happygallery.application.payment.port.in.PaymentPrepareUseCase.PrepareResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 prepare / confirm 단일 진입점.
 *
 * <p>주문/예약/8회권 모두 이 컨트롤러를 통해 결제를 시작한다.
 * 회원/비회원 구분은 {@link CustomerUserId} resolver의 nullability로 결정된다.
 */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentPrepareUseCase prepareUseCase;
    private final PaymentConfirmUseCase confirmUseCase;

    public PaymentController(PaymentPrepareUseCase prepareUseCase,
                             PaymentConfirmUseCase confirmUseCase) {
        this.prepareUseCase = prepareUseCase;
        this.confirmUseCase = confirmUseCase;
    }

    @PostMapping("/prepare")
    public PreparePaymentResponse prepare(@RequestBody @Valid PreparePaymentRequest req,
                                          @CustomerUserId Long userId) {
        AuthContext auth = userId != null ? AuthContext.member(userId) : AuthContext.guest();
        PrepareResult result = prepareUseCase.prepare(new PrepareCommand(req.context(), req.payload(), auth));
        return PreparePaymentResponse.from(result);
    }

    @PostMapping("/confirm")
    public ConfirmPaymentResponse confirm(@RequestBody @Valid ConfirmPaymentRequest req,
                                          @CustomerUserId Long userId) {
        AuthContext auth = userId != null ? AuthContext.member(userId) : AuthContext.guest();
        ConfirmResult result = confirmUseCase.confirm(
                new ConfirmCommand(req.paymentKey(), req.orderId(), req.amount(), auth));
        return ConfirmPaymentResponse.from(result);
    }
}
