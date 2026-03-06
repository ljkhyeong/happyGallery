package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.product.ProductAdminService;
import com.personal.happygallery.app.product.ProductQueryService;
import com.personal.happygallery.app.web.admin.dto.CreateProductRequest;
import com.personal.happygallery.app.web.admin.dto.ProductResponse;
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
@RequestMapping({"/api/v1/admin/products", "/admin/products"})
public class AdminProductController {

    private final ProductAdminService productAdminService;
    private final ProductQueryService productQueryService;

    public AdminProductController(ProductAdminService productAdminService,
                                  ProductQueryService productQueryService) {
        this.productAdminService = productAdminService;
        this.productQueryService = productQueryService;
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
        return productQueryService.listActiveProducts().stream()
                .map(r -> ProductResponse.from(r.product(), r.inventory()))
                .toList();
    }
}
