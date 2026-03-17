package com.personal.happygallery.app.web.product;

import com.personal.happygallery.app.qna.ProductQnaService;
import com.personal.happygallery.app.qna.ProductQnaService.QnaWithAuthor;
import com.personal.happygallery.domain.qna.ProductQna;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
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

    private final ProductQnaService qnaService;

    public ProductQnaController(ProductQnaService qnaService) {
        this.qnaService = qnaService;
    }

    @GetMapping
    public List<ProductQnaListItem> list(@PathVariable Long productId) {
        return qnaService.listByProduct(productId).stream()
                .map(ProductQnaListItem::from)
                .toList();
    }

    @PostMapping("/{id}/verify")
    public ProductQnaDetail verify(@PathVariable Long productId,
                                   @PathVariable Long id,
                                   @RequestBody @Valid VerifyQnaPasswordRequest request) {
        QnaWithAuthor result = qnaService.verifyAndGet(id, request.password());
        return ProductQnaDetail.from(result);
    }

    // ── DTO ──

    public record ProductQnaListItem(
            Long id, String title, String authorName, boolean secret,
            boolean hasReply, LocalDateTime createdAt
    ) {
        static ProductQnaListItem from(QnaWithAuthor qa) {
            ProductQna q = qa.qna();
            String displayTitle = q.isSecret() ? "[비밀글입니다]" : q.getTitle();
            return new ProductQnaListItem(
                    q.getId(), displayTitle, maskName(qa.authorName()),
                    q.isSecret(), q.hasReply(), q.getCreatedAt());
        }
    }

    public record ProductQnaDetail(
            Long id, Long productId, String title, String content,
            String replyContent, LocalDateTime repliedAt,
            boolean secret, String authorName, LocalDateTime createdAt
    ) {
        static ProductQnaDetail from(QnaWithAuthor qa) {
            ProductQna q = qa.qna();
            return new ProductQnaDetail(
                    q.getId(), q.getProductId(), q.getTitle(), q.getContent(),
                    q.getReplyContent(), q.getRepliedAt(),
                    q.isSecret(), maskName(qa.authorName()), q.getCreatedAt());
        }
    }

    public record VerifyQnaPasswordRequest(@NotBlank String password) {}

    private static String maskName(String name) {
        if (name == null || name.length() <= 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }
}
