package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.qna.ProductQnaService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.CreateQnaRequest;
import com.personal.happygallery.app.web.customer.dto.QnaCreatedResponse;
import jakarta.servlet.http.HttpServletRequest;
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

    private final ProductQnaService qnaService;

    public MeProductQnaController(ProductQnaService qnaService) {
        this.qnaService = qnaService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QnaCreatedResponse create(@PathVariable Long productId,
                                     @RequestBody @Valid CreateQnaRequest request,
                                     HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return QnaCreatedResponse.from(qnaService.createQuestion(
                productId, userId, request.title(), request.content(),
                request.secret(), request.password()));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}
