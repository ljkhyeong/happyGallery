package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.AdjustInventoryCommand;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.ProductInventoryResult;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.adapter.in.web.admin.dto.AdjustInventoryRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateProductRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.InventoryAdjustmentResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.ProductResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateProductStatusRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateProductRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductAdminUseCase productAdminUseCase;
    private final ProductQueryUseCase productQueryUseCase;

    public AdminProductController(ProductAdminUseCase productAdminUseCase,
                                  ProductQueryUseCase productQueryUseCase) {
        this.productAdminUseCase = productAdminUseCase;
        this.productQueryUseCase = productQueryUseCase;
    }

    /** POST /api/v1/admin/products — 상품 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse register(@RequestBody @Valid CreateProductRequest request) {
        ProductInventoryResult result = productAdminUseCase.register(
                request.name(), request.type(), request.category(), request.price(), request.quantity(),
                request.description(), request.imageUrl());
        return ProductResponse.from(result);
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "updateAdminProduct")
    public ProductResponse update(@PathVariable Long id,
                                  @RequestBody @Valid UpdateProductRequest request) {
        return ProductResponse.from(productAdminUseCase.update(
                id, request.name(), request.category(), request.price(),
                request.description(), request.imageUrl()));
    }

    /** GET /api/v1/admin/products — 판매 중지 상품을 포함한 전체 목록 */
    @GetMapping
    public List<ProductResponse> listAll() {
        return productQueryUseCase.listAllProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/status")
    public ProductResponse changeStatus(@PathVariable Long id,
                                        @RequestBody @Valid UpdateProductStatusRequest request) {
        return ProductResponse.from(productAdminUseCase.changeStatus(id, request.status()));
    }

    @PostMapping("/{id}/inventory-adjustments")
    public InventoryAdjustmentResponse adjustInventory(
            @PathVariable Long id,
            @RequestBody @Valid AdjustInventoryRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return InventoryAdjustmentResponse.from(productAdminUseCase.adjustInventory(new AdjustInventoryCommand(
                id,
                request.type(),
                request.quantity(),
                request.reason(),
                admin.adminUserId(),
                admin.getName())));
    }

    @GetMapping("/{id}/inventory-adjustments")
    public List<InventoryAdjustmentResponse> listInventoryAdjustments(@PathVariable Long id) {
        return productAdminUseCase.listRecentInventoryAdjustments(id).stream()
                .map(InventoryAdjustmentResponse::from)
                .toList();
    }
}
