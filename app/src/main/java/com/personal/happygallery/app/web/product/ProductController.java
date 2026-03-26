package com.personal.happygallery.app.web.product;

import com.personal.happygallery.app.product.ProductFilter;
import com.personal.happygallery.app.product.ProductFilter.ProductSortOrder;
import com.personal.happygallery.app.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.app.web.product.dto.ProductDetailResponse;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/products", "/products"})
public class ProductController {

    private static final int MAX_KEYWORD_LENGTH = 100;

    private final ProductQueryUseCase productQueryUseCase;

    public ProductController(ProductQueryUseCase productQueryUseCase) {
        this.productQueryUseCase = productQueryUseCase;
    }

    /** GET /products — ACTIVE 상품 목록 (필터·정렬 지원) */
    @GetMapping
    public List<ProductDetailResponse> listProducts(
            @RequestParam(required = false) ProductType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "newest") String sort) {

        String cleanKeyword = clampKeyword(keyword);
        ProductSortOrder sortOrder = parseSortOrder(sort);

        ProductFilter filter = new ProductFilter(type, category, cleanKeyword, sortOrder);

        return productQueryUseCase.listActiveProducts(filter).stream()
                .map(ProductDetailResponse::from)
                .toList();
    }

    /** GET /products/categories — ACTIVE 상품 카테고리 목록 */
    @GetMapping("/categories")
    public List<String> listCategories() {
        return productQueryUseCase.listActiveCategories();
    }

    /** GET /products/{id} — 상품 상세 + 재고 가용 여부 */
    @GetMapping("/{id}")
    public ProductDetailResponse getProduct(@PathVariable Long id) {
        return ProductDetailResponse.from(productQueryUseCase.getProduct(id));
    }

    private static String clampKeyword(String keyword) {
        if (keyword == null) return null;
        String trimmed = keyword.trim();
        if (trimmed.isEmpty()) return null;
        return trimmed.length() > MAX_KEYWORD_LENGTH
                ? trimmed.substring(0, MAX_KEYWORD_LENGTH)
                : trimmed;
    }

    private static ProductSortOrder parseSortOrder(String sort) {
        return switch (sort) {
            case "price_asc" -> ProductSortOrder.PRICE_ASC;
            case "price_desc" -> ProductSortOrder.PRICE_DESC;
            default -> ProductSortOrder.NEWEST;
        };
    }
}
