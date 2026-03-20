package com.personal.happygallery.app.web.product;

import com.personal.happygallery.app.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.app.qna.port.in.ProductQnaUseCase.QnaWithAuthor;
import com.personal.happygallery.app.web.product.dto.ProductQnaDetail;
import com.personal.happygallery.app.web.product.dto.ProductQnaListItem;
import com.personal.happygallery.app.web.product.dto.VerifyQnaPasswordRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/products/{productId}/qna", "/products/{productId}/qna"})
public class ProductQnaController {

    private final ProductQnaUseCase qnaUseCase;

    public ProductQnaController(ProductQnaUseCase qnaUseCase) {
        this.qnaUseCase = qnaUseCase;
    }

    @GetMapping
    public List<ProductQnaListItem> list(@PathVariable Long productId) {
        return qnaUseCase.listByProduct(productId).stream()
                .map(ProductQnaListItem::from)
                .toList();
    }

    @PostMapping("/{id}/verify")
    public ProductQnaDetail verify(@PathVariable Long productId,
                                   @PathVariable Long id,
                                   @RequestBody @Valid VerifyQnaPasswordRequest request) {
        QnaWithAuthor result = qnaUseCase.verifyAndGet(id, request.password());
        return ProductQnaDetail.from(result);
    }
}
