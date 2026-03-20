package com.personal.happygallery.app.web.product;

import com.personal.happygallery.app.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.app.product.port.in.ProductQueryUseCase.ProductWithInventory;
import com.personal.happygallery.app.web.product.dto.ProductDetailResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/products", "/products"})
public class ProductController {

    private final ProductQueryUseCase productQueryUseCase;

    public ProductController(ProductQueryUseCase productQueryUseCase) {
        this.productQueryUseCase = productQueryUseCase;
    }

    /** GET /products — ACTIVE 상품 목록 */
    @GetMapping
    public List<ProductDetailResponse> listProducts() {
        return productQueryUseCase.listActiveProducts().stream()
                .map(ProductDetailResponse::from)
                .toList();
    }

    /** GET /products/{id} — 상품 상세 + 재고 가용 여부 */
    @GetMapping("/{id}")
    public ProductDetailResponse getProduct(@PathVariable Long id) {
        ProductWithInventory result = productQueryUseCase.getProduct(id);
        return ProductDetailResponse.from(result);
    }
}
