package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.product.port.in.ProductAdminUseCase;
import com.personal.happygallery.application.product.port.in.ProductAdminUseCase.RegisterResult;
import com.personal.happygallery.application.product.port.in.ProductQueryUseCase;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateProductRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.ProductResponse;
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

    private final ProductAdminUseCase productAdminUseCase;
    private final ProductQueryUseCase productQueryUseCase;

    public AdminProductController(ProductAdminUseCase productAdminUseCase,
                                  ProductQueryUseCase productQueryUseCase) {
        this.productAdminUseCase = productAdminUseCase;
        this.productQueryUseCase = productQueryUseCase;
    }

    /** POST /admin/products — 상품 등록 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse register(@RequestBody @Valid CreateProductRequest request) {
        RegisterResult result = productAdminUseCase.register(
                request.name(), request.type(), request.category(), request.price(), request.quantity());
        return ProductResponse.from(result);
    }

    /** GET /admin/products — ACTIVE 상품 목록 */
    @GetMapping
    public List<ProductResponse> listActive() {
        return productQueryUseCase.listActiveProducts().stream()
                .map(ProductResponse::from)
                .toList();
    }
}
