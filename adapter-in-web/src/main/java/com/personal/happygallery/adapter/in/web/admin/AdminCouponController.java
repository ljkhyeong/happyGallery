package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminCouponResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateCouponRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateCouponRequest;
import com.personal.happygallery.application.coupon.port.in.CouponAdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/coupons")
public class AdminCouponController {

    private final CouponAdminUseCase couponAdminUseCase;

    public AdminCouponController(CouponAdminUseCase couponAdminUseCase) {
        this.couponAdminUseCase = couponAdminUseCase;
    }

    @GetMapping
    @Operation(operationId = "listAdminCoupons")
    public List<AdminCouponResponse> list() {
        return couponAdminUseCase.list().stream()
                .map(AdminCouponResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getAdminCoupon")
    public AdminCouponResponse get(@PathVariable Long id) {
        return AdminCouponResponse.from(couponAdminUseCase.get(id));
    }

    @PostMapping
    @Operation(operationId = "createAdminCoupon")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminCouponResponse create(@RequestBody @Valid CreateCouponRequest request) {
        return AdminCouponResponse.from(couponAdminUseCase.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateAdminCoupon")
    public AdminCouponResponse update(@PathVariable Long id,
                                      @RequestBody @Valid UpdateCouponRequest request) {
        return AdminCouponResponse.from(couponAdminUseCase.update(
                id,
                request.expectedVersion(),
                request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteAdminCoupon")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id,
                       @RequestParam @PositiveOrZero long expectedVersion) {
        couponAdminUseCase.delete(id, expectedVersion);
    }
}
