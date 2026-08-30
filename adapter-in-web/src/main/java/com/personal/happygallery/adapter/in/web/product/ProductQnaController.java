package com.personal.happygallery.adapter.in.web.product;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.adapter.in.web.product.dto.ProductQnaDetail;
import com.personal.happygallery.adapter.in.web.product.dto.ProductQnaListItem;
import com.personal.happygallery.adapter.in.web.product.dto.ProductQnaPageResponse;
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
@RequestMapping("/api/v1/products/{productId}/qna")
public class ProductQnaController {

    private final ProductQnaUseCase qnaUseCase;

    public ProductQnaController(ProductQnaUseCase qnaUseCase) {
        this.qnaUseCase = qnaUseCase;
    }

    @GetMapping
    @Operation(operationId = "listProductQna")
    public List<ProductQnaListItem> list(@PathVariable Long productId) {
        return qnaUseCase.listByProduct(productId).stream()
                .map(ProductQnaListItem::from)
                .toList();
    }

    @GetMapping("/page")
    @Operation(operationId = "listProductQnaPage")
    public ProductQnaPageResponse listPage(
            @PathVariable Long productId,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return ProductQnaPageResponse.from(
                qnaUseCase.listByProduct(productId, cursor, size));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getPublicProductQna")
    public ProductQnaDetail getPublicDetail(@PathVariable Long productId,
                                            @PathVariable Long id) {
        return ProductQnaDetail.from(qnaUseCase.getPublicDetail(productId, id));
    }
}
