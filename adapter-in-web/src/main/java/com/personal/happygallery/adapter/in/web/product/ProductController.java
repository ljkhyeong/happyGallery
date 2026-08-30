package com.personal.happygallery.adapter.in.web.product;

import com.personal.happygallery.application.product.ProductFilter;
import com.personal.happygallery.application.product.ProductFilter.ProductSortOrder;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.application.search.SearchParams;
import com.personal.happygallery.adapter.in.web.product.dto.ProductDetailResponse;
import com.personal.happygallery.domain.product.ProductType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductQueryUseCase productQueryUseCase;

    public ProductController(ProductQueryUseCase productQueryUseCase) {
        this.productQueryUseCase = productQueryUseCase;
    }

    /** GET /api/v1/products — ACTIVE 상품 목록 (필터·정렬 지원) */
    @Operation(operationId = "listProducts")
    @GetMapping
    public List<ProductDetailResponse> listProducts(
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @Parameter(schema = @Schema(allowableValues = {"newest", "price_asc", "price_desc"}))
            @RequestParam(required = false, defaultValue = "newest") String sort) {

        String cleanKeyword = SearchParams.clampKeyword(keyword);
        ProductSortOrder sortOrder = ProductSortOrder.fromParam(sort);

        ProductFilter filter = new ProductFilter(type, category, cleanKeyword, sortOrder);

        return productQueryUseCase.listActiveProducts(filter).stream()
                .map(ProductDetailResponse::from)
                .toList();
    }

    /** GET /api/v1/products/categories — ACTIVE 상품 카테고리 목록 */
    @Operation(operationId = "listProductCategories")
    @GetMapping("/categories")
    public List<String> listCategories() {
        return productQueryUseCase.listActiveCategories();
    }

    /** GET /api/v1/products/{id} — 상품 상세 + 재고 가용 여부 */
    @Operation(operationId = "getProduct")
    @GetMapping("/{id}")
    public ProductDetailResponse getProduct(@PathVariable Long id) {
        return ProductDetailResponse.from(productQueryUseCase.getProduct(id));
    }
}
