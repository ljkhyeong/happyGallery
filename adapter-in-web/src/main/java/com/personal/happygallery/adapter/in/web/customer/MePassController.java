package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.MyPassSummary;
import com.personal.happygallery.adapter.in.web.customer.dto.MemberPassRefundResponse;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.application.pass.port.in.MemberPassRefundUseCase;
import com.personal.happygallery.application.pass.port.in.PassQueryUseCase;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 8회권 조회·정산 환불 API.
 *
 * <p>8회권 구매는 {@code POST /api/v1/payments/prepare} → {@code /confirm} 경로로 일원화됨.
 */
@RestController
@RequestMapping("/api/v1/me/passes")
public class MePassController {

    private final PassQueryUseCase passQueryUseCase;
    private final MemberPassRefundUseCase memberPassRefundUseCase;
    private final SubjectRateLimitGuard rateLimitGuard;

    public MePassController(PassQueryUseCase passQueryUseCase,
                            MemberPassRefundUseCase memberPassRefundUseCase,
                            SubjectRateLimitGuard rateLimitGuard) {
        this.passQueryUseCase = passQueryUseCase;
        this.memberPassRefundUseCase = memberPassRefundUseCase;
        this.rateLimitGuard = rateLimitGuard;
    }

    @GetMapping
    public List<MyPassSummary> myPasses(@AuthenticationPrincipal CustomerPrincipal customer) {
        return MyPassSummary.fromAll(passQueryUseCase.listMyPasses(customer.userId()));
    }

    @GetMapping("/{id}")
    public MyPassSummary myPass(@PathVariable Long id,
                                @AuthenticationPrincipal CustomerPrincipal customer) {
        return MyPassSummary.from(passQueryUseCase.findMyPass(id, customer.userId()));
    }

    @PostMapping("/{id}/refund")
    public MemberPassRefundResponse refundMyPass(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkPassRefund(customer.userId());
        return MemberPassRefundResponse.from(memberPassRefundUseCase.refundMyPass(id, customer.userId()));
    }
}
