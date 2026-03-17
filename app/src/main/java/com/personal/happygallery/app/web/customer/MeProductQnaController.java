package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.qna.ProductQnaService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.domain.qna.ProductQna;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
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
        ProductQna qna = qnaService.createQuestion(
                productId, userId, request.title(), request.content(),
                request.secret(), request.password());
        return QnaCreatedResponse.from(qna);
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }

    // ── DTO ──

    public record CreateQnaRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            boolean secret,
            @Size(min = 4, max = 20) String password
    ) {}

    public record QnaCreatedResponse(Long id, Long productId, String title,
                                     boolean secret, LocalDateTime createdAt) {
        static QnaCreatedResponse from(ProductQna q) {
            return new QnaCreatedResponse(q.getId(), q.getProductId(),
                    q.getTitle(), q.isSecret(), q.getCreatedAt());
        }
    }
}
