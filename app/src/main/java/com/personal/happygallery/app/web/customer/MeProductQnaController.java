package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.app.web.customer.dto.CreateQnaRequest;
import com.personal.happygallery.app.web.customer.dto.QnaCreatedResponse;
import com.personal.happygallery.app.web.resolver.CustomerUserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @ResponseStatus(HttpStatus.CREATED)
    public QnaCreatedResponse create(@PathVariable Long productId,
                                     @RequestBody @Valid CreateQnaRequest request,
                                     @CustomerUserId Long userId) {
        return QnaCreatedResponse.from(qnaUseCase.createQuestion(
                productId, userId, request.title(), request.content(),
                request.secret(), request.password()));
    }
}
