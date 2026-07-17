package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.pass.port.in.PassQueryUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.MyPassSummary;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.domain.pass.PassPurchase;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 8회권 조회 API.
 *
 * <p>8회권 구매는 {@code POST /api/v1/payments/prepare} → {@code /confirm} 경로로 일원화됨.
 */
@RestController
@RequestMapping("/api/v1/me/passes")
public class MePassController {

    private final PassQueryUseCase passQueryUseCase;

    public MePassController(PassQueryUseCase passQueryUseCase) {
        this.passQueryUseCase = passQueryUseCase;
    }

    @GetMapping
    public List<MyPassSummary> myPasses(@AuthenticationPrincipal CustomerPrincipal customer) {
        return MyPassSummary.fromAll(passQueryUseCase.listMyPasses(customer.userId()));
    }

    @GetMapping("/{id}")
    public MyPassSummary myPass(@PathVariable Long id,
                                @AuthenticationPrincipal CustomerPrincipal customer) {
        PassPurchase pass = passQueryUseCase.findMyPass(id, customer.userId());
        return MyPassSummary.from(pass);
    }
}
