package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.product.ProductAdminService;
import com.personal.happygallery.app.web.admin.dto.CreateProductRequest;
import com.personal.happygallery.app.web.admin.dto.ProductResponse;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.product.Inventory;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.infra.product.InventoryRepository;
import com.personal.happygallery.infra.product.ProductRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/products")
public class AdminProductController {

    private final ProductAdminService productAdminService;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public AdminProductController(ProductAdminService productAdminService,
                                  ProductRepository productRepository,
                                  InventoryRepository inventoryRepository) {
        this.productAdminService = productAdminService;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /** POST /admin/products — 상품 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse register(@RequestBody @Valid CreateProductRequest request) {
        ProductAdminService.RegisterResult result = productAdminService.register(
                request.name(), request.type(), request.price(), request.quantity());
        return ProductResponse.from(result.product(), result.inventory());
    }

    /** GET /admin/products — ACTIVE 상품 목록 */
    @GetMapping
    public List<ProductResponse> listActive() {
        List<Product> products = productRepository.findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE);
        return products.stream()
                .map(p -> {
                    Inventory inv = inventoryRepository.findByProductId(p.getId())
                            .orElseThrow(() -> new NotFoundException("재고"));
                    return ProductResponse.from(p, inv);
                })
                .toList();
    }
}
