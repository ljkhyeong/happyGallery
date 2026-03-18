package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.qna.ProductQnaService.QnaWithAuthor;
import com.personal.happygallery.app.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.app.web.AdminAuthFilter;
import com.personal.happygallery.app.web.admin.dto.AdminQnaResponse;
import com.personal.happygallery.app.web.admin.dto.QnaReplyRequest;
import com.personal.happygallery.domain.qna.ProductQna;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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

    private final ProductQnaUseCase qnaService;

    public AdminProductQnaController(ProductQnaUseCase qnaService) {
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
}
