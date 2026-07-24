package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase.InquiryWithUser;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminInquiryResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.InquiryReplyRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.shared.page.CursorPage;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/inquiries")
public class AdminInquiryController {

    private final InquiryUseCase inquiryUseCase;

    public AdminInquiryController(InquiryUseCase inquiryUseCase) {
        this.inquiryUseCase = inquiryUseCase;
    }

    @GetMapping
    @Operation(operationId = "listAdminInquiries")
    public CursorPage<AdminInquiryResponse> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        CursorPage<InquiryWithUser> page = inquiryUseCase.listAll(cursor, size);
        return new CursorPage<>(
                page.content().stream().map(AdminInquiryResponse::from).toList(),
                page.nextCursor(),
                page.hasMore());
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getAdminInquiry")
    public AdminInquiryResponse detail(@PathVariable Long id) {
        InquiryWithUser inquiry = inquiryUseCase.findByIdForAdmin(id);
        return AdminInquiryResponse.from(inquiry);
    }

    @PostMapping("/{id}/reply")
    @Operation(operationId = "replyToAdminInquiry")
    public AdminInquiryResponse reply(@PathVariable Long id,
                                      @RequestBody @Valid InquiryReplyRequest request,
                                      @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminInquiryResponse.from(inquiryUseCase.replyAndGet(
                id, request.replyContent(), admin.adminUserId()));
    }
}
