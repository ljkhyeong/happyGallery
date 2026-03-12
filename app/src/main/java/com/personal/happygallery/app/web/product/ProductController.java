package com.personal.happygallery.app.web.product;

import com.personal.happygallery.app.product.ProductQueryService;
import com.personal.happygallery.app.web.product.dto.ProductDetailResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/products", "/products"})
public class ProductController {

    private final ProductQueryService productQueryService;

    public ProductController(ProductQueryService productQueryService) {
        this.productQueryService = productQueryService;
    }

    /** GET /products — ACTIVE 상품 목록 */
    @GetMapping
    public List<ProductDetailResponse> listProducts() {
        return productQueryService.listActiveProducts().stream()
                .map(r -> ProductDetailResponse.from(r.product(), r.inventory()))
                .toList();
    }

    /** GET /products/{id} — 상품 상세 + 재고 가용 여부 */
    @GetMapping("/{id}")
    public ProductDetailResponse getProduct(@PathVariable Long id) {
        ProductQueryService.ProductWithInventory result = productQueryService.getProduct(id);
        return ProductDetailResponse.from(result.product(), result.inventory());
    }
}
