package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.qna.ProductQnaService;
import com.personal.happygallery.app.qna.ProductQnaService.QnaWithAuthor;
import com.personal.happygallery.app.web.AdminAuthFilter;
import com.personal.happygallery.domain.qna.ProductQna;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/qna", "/admin/qna"})
public class AdminProductQnaController {

    private final ProductQnaService qnaService;

    public AdminProductQnaController(ProductQnaService qnaService) {
        this.qnaService = qnaService;
    }

    @GetMapping
    public List<AdminQnaResponse> list(@RequestParam Long productId) {
        return qnaService.listByProduct(productId).stream()
                .map(AdminQnaResponse::from)
                .toList();
    }

    @PostMapping("/{id}/reply")
    public AdminQnaResponse reply(@PathVariable Long id,
                                  @RequestBody @Valid QnaReplyRequest request,
                                  HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute(AdminAuthFilter.ADMIN_USER_ID_ATTR);
        ProductQna qna = qnaService.reply(id, request.replyContent(), adminId);
        String authorName = qnaService.listByProduct(qna.getProductId()).stream()
                .filter(q -> q.qna().getId().equals(qna.getId()))
                .map(QnaWithAuthor::authorName)
                .findFirst().orElse("탈퇴회원");
        return AdminQnaResponse.from(new QnaWithAuthor(qna, authorName));
    }

    // ── DTO ──

    public record AdminQnaResponse(
            Long id, Long productId, Long userId, String authorName,
            String title, String content, boolean secret,
            String replyContent, LocalDateTime repliedAt, LocalDateTime createdAt
    ) {
        static AdminQnaResponse from(QnaWithAuthor qa) {
            ProductQna q = qa.qna();
            return new AdminQnaResponse(
                    q.getId(), q.getProductId(), q.getUserId(), qa.authorName(),
                    q.getTitle(), q.getContent(), q.isSecret(),
                    q.getReplyContent(), q.getRepliedAt(), q.getCreatedAt());
        }
    }

    public record QnaReplyRequest(@NotBlank String replyContent) {}
}
