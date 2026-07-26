package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminQnaPageResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminQnaResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.QnaReplyRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/qna")
public class AdminProductQnaController {

    private final ProductQnaUseCase qnaUseCase;

    public AdminProductQnaController(ProductQnaUseCase qnaUseCase) {
        this.qnaUseCase = qnaUseCase;
    }

    @GetMapping
    @Operation(operationId = "listAdminProductQna")
    public List<AdminQnaResponse> list(@RequestParam Long productId) {
        return qnaUseCase.listByProduct(productId).stream()
                .map(AdminQnaResponse::from)
                .toList();
    }

    @GetMapping("/unanswered")
    @Operation(operationId = "listUnansweredAdminProductQna")
    public AdminQnaPageResponse listUnanswered(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return AdminQnaPageResponse.from(qnaUseCase.listUnanswered(cursor, size));
    }

    @PostMapping("/{id}/reply")
    @Operation(operationId = "replyProductQna")
    public AdminQnaResponse reply(@PathVariable Long id,
                                  @RequestBody @Valid QnaReplyRequest request,
                                  @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminQnaResponse.from(qnaUseCase.replyAndGet(
                id, request.replyContent(), admin.adminUserId()));
    }
}
