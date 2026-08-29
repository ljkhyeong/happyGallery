package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdjustInventoryRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.ApplySmartStoreProductRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateProductRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.InventoryAdjustmentResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.ProductResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SaveSmartStoreInventoryMappingRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreInventoryMappingResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreProductPreviewResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateProductStatusRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateProductRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.AdjustInventoryCommand;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.ProductResult;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.product.port.in.SmartStoreInventoryUseCase;
import com.personal.happygallery.domain.error.NotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {

    private final ProductAdminUseCase productAdminUseCase;
    private final ProductQueryUseCase productQueryUseCase;
    private final SmartStoreInventoryUseCase smartStoreInventoryUseCase;

    public AdminProductController(ProductAdminUseCase productAdminUseCase,
                                  ProductQueryUseCase productQueryUseCase,
                                  SmartStoreInventoryUseCase smartStoreInventoryUseCase) {
        this.productAdminUseCase = productAdminUseCase;
        this.productQueryUseCase = productQueryUseCase;
        this.smartStoreInventoryUseCase = smartStoreInventoryUseCase;
    }

    /** POST /api/v1/admin/products — 상품 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse register(@RequestBody @Valid CreateProductRequest request) {
        ProductResult result = productAdminUseCase.register(request.toCommand());
        return ProductResponse.from(result);
    }

    @PatchMapping("/{id}")
    @Operation(operationId = "updateAdminProduct")
    public ProductResponse update(@PathVariable Long id,
                                  @RequestBody @Valid UpdateProductRequest request) {
        return ProductResponse.from(productAdminUseCase.update(id, request.toCommand()));
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
                request.productVariantId(),
                request.type(),
                request.quantity(),
                request.reason(),
                admin.auditActorId(),
                admin.getName())));
    }

    @GetMapping("/{id}/inventory-adjustments")
    public List<InventoryAdjustmentResponse> listInventoryAdjustments(@PathVariable Long id) {
        return productAdminUseCase.listRecentInventoryAdjustments(id).stream()
                .map(InventoryAdjustmentResponse::from)
                .toList();
    }

    @PutMapping("/{id}/smartstore-inventory")
    @Operation(operationId = "saveSmartStoreInventoryMapping")
    public SmartStoreInventoryMappingResponse saveSmartStoreInventoryMapping(
            @PathVariable Long id,
            @RequestBody @Valid SaveSmartStoreInventoryMappingRequest request) {
        return SmartStoreInventoryMappingResponse.from(
                smartStoreInventoryUseCase.saveMapping(id, request.toCommand()));
    }

    @GetMapping("/{id}/smartstore-inventory")
    @Operation(operationId = "getSmartStoreInventoryMapping")
    public SmartStoreInventoryMappingResponse getSmartStoreInventoryMapping(@PathVariable Long id) {
        return smartStoreInventoryUseCase.getMapping(id)
                .map(SmartStoreInventoryMappingResponse::from)
                .orElseThrow(NotFoundException.supplier("스마트스토어 재고 연동 설정"));
    }

    @DeleteMapping("/{id}/smartstore-inventory")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteSmartStoreInventoryMapping")
    public void deleteSmartStoreInventoryMapping(@PathVariable Long id) {
        smartStoreInventoryUseCase.deleteMapping(id);
    }

    @PostMapping("/{id}/smartstore-inventory/retry")
    @Operation(operationId = "retrySmartStoreInventorySync")
    public SmartStoreInventoryMappingResponse retrySmartStoreInventorySync(@PathVariable Long id) {
        return SmartStoreInventoryMappingResponse.from(smartStoreInventoryUseCase.retry(id));
    }

    @GetMapping("/{id}/smartstore-product-preview")
    @Operation(operationId = "previewSmartStoreProductSync")
    public SmartStoreProductPreviewResponse previewSmartStoreProductSync(@PathVariable Long id) {
        return SmartStoreProductPreviewResponse.from(
                smartStoreInventoryUseCase.previewProduct(id));
    }

    @PostMapping("/{id}/smartstore-product-sync")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "applySmartStoreProductSync")
    public void applySmartStoreProductSync(
            @PathVariable Long id,
            @Valid @RequestBody ApplySmartStoreProductRequest request) {
        smartStoreInventoryUseCase.applyProduct(id, request.productVersion());
    }
}
