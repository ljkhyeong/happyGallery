package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.CreateQnaRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.MyProductQnaPageResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.QnaCreatedResponse;
import com.personal.happygallery.adapter.in.web.product.dto.ProductQnaDetail;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/products/{productId}/qna")
public class MeProductQnaController {

    private final ProductQnaUseCase qnaUseCase;

    public MeProductQnaController(ProductQnaUseCase qnaUseCase) {
        this.qnaUseCase = qnaUseCase;
    }

    @PostMapping
    @Operation(operationId = "createProductQna")
    @ResponseStatus(HttpStatus.CREATED)
    public QnaCreatedResponse create(@PathVariable Long productId,
                                     @RequestBody @Valid CreateQnaRequest request,
                                     @AuthenticationPrincipal CustomerPrincipal customer) {
        var created = qnaUseCase.createQuestion(
                productId, customer.userId(), request.title(), request.content(), request.secret());
        // 작성 시각은 DB 기본값이라 저장한 엔티티에는 비어 있다. 저장이 끝난 뒤 다시 읽어 응답한다.
        return QnaCreatedResponse.from(
                qnaUseCase.getOwnedDetail(productId, created.getId(), customer.userId()).qna());
    }

    @GetMapping("/page")
    @Operation(operationId = "listMyProductQnaPage")
    public MyProductQnaPageResponse listPage(
            @PathVariable Long productId,
            @AuthenticationPrincipal CustomerPrincipal customer,
            @RequestParam(required = false) String cursor,
            @Parameter(schema = @Schema(
                    type = "integer", format = "int32", defaultValue = "20",
                    minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return MyProductQnaPageResponse.from(qnaUseCase.listOwnedByProduct(
                productId, customer.userId(), cursor, size));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getMyProductQna")
    public ProductQnaDetail detail(@PathVariable Long productId,
                                   @PathVariable Long id,
                                   @AuthenticationPrincipal CustomerPrincipal customer) {
        return ProductQnaDetail.from(
                qnaUseCase.getOwnedDetail(productId, id, customer.userId()));
    }
}
