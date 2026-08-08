package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.customer.dto.ClaimCouponRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.ClaimableCouponResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.MyCouponResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.coupon.port.in.CouponMemberUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/coupons")
public class MeCouponController {

    private final CouponMemberUseCase couponMemberUseCase;

    public MeCouponController(CouponMemberUseCase couponMemberUseCase) {
        this.couponMemberUseCase = couponMemberUseCase;
    }

    @GetMapping
    @Operation(operationId = "listMyCoupons")
    public List<MyCouponResponse> list(
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return couponMemberUseCase.listMyCoupons(customer.userId()).stream()
                .map(MyCouponResponse::from)
                .toList();
    }

    @GetMapping("/claimable")
    @Operation(operationId = "listClaimableCoupons")
    public List<ClaimableCouponResponse> listClaimable(
            @AuthenticationPrincipal CustomerPrincipal customer) {
        return couponMemberUseCase.listClaimableCoupons(customer.userId()).stream()
                .map(ClaimableCouponResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(operationId = "claimMyCoupon")
    @ResponseStatus(HttpStatus.CREATED)
    public MyCouponResponse claim(
            @AuthenticationPrincipal CustomerPrincipal customer,
            @RequestBody @Valid ClaimCouponRequest request) {
        return MyCouponResponse.from(
                couponMemberUseCase.claim(customer.userId(), request.definitionId()));
    }
}
