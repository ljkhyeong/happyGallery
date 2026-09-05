package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.pass.PassHistoryQuery;
import com.personal.happygallery.application.pass.PassHistoryQuery.PassHistorySort;
import com.personal.happygallery.application.pass.PassHistoryQuery.PassHistoryStatus;
import com.personal.happygallery.adapter.in.web.customer.dto.MyPassSummary;
import com.personal.happygallery.adapter.in.web.customer.dto.MyPassPageResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.MemberPassRefundResponse;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.application.pass.port.in.MemberPassRefundUseCase;
import com.personal.happygallery.application.pass.port.in.PassQueryUseCase;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    @Operation(operationId = "listMyPasses")
    public List<MyPassSummary> myPasses(@AuthenticationPrincipal CustomerPrincipal customer) {
        return MyPassSummary.fromAll(passQueryUseCase.listMyPasses(customer.userId()));
    }

    @GetMapping("/page")
    @Operation(operationId = "listMyPassesPage")
    public MyPassPageResponse myPassesPage(
            @AuthenticationPrincipal CustomerPrincipal customer,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) PassHistoryStatus status,
            @RequestParam(defaultValue = "PURCHASE_DESC") PassHistorySort sort) {
        return MyPassPageResponse.from(
                passQueryUseCase.listMyPasses(customer.userId(), new PassHistoryQuery(keyword, status, sort), cursor, size));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getMyPass")
    public MyPassSummary myPass(@PathVariable Long id,
                                @AuthenticationPrincipal CustomerPrincipal customer) {
        return MyPassSummary.from(passQueryUseCase.findMyPass(id, customer.userId()));
    }

    @PostMapping("/{id}/refund")
    @Operation(operationId = "refundMyPass")
    public MemberPassRefundResponse refundMyPass(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomerPrincipal customer) {
        rateLimitGuard.checkPassRefund(customer.userId());
        return MemberPassRefundResponse.from(memberPassRefundUseCase.refundMyPass(id, customer.userId()));
    }
}
