package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.RewardWalletResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.reward.port.in.RewardQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/rewards")
public class MeRewardController {

    private final RewardQueryUseCase rewardQueryUseCase;

    public MeRewardController(RewardQueryUseCase rewardQueryUseCase) {
        this.rewardQueryUseCase = rewardQueryUseCase;
    }

    @GetMapping
    @Operation(operationId = "getMyRewardWallet")
    public RewardWalletResponse wallet(@AuthenticationPrincipal CustomerPrincipal customer) {
        return RewardWalletResponse.from(rewardQueryUseCase.getWallet(customer.userId()));
    }
}
