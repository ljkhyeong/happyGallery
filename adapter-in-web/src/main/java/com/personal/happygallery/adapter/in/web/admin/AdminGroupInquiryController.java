package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminGroupInquiryResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.GroupInquiryUpdateRequest;
import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryPageResponse;
import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/group-inquiries")
public class AdminGroupInquiryController {
    private final GroupInquiryUseCase inquiries;
    public AdminGroupInquiryController(GroupInquiryUseCase inquiries) { this.inquiries = inquiries; }

    @GetMapping
    @Operation(operationId = "listAdminGroupInquiries")
    public GroupInquiryPageResponse list(@RequestParam(required = false) GroupInquiryStatus status,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int size) {
        return GroupInquiryPageResponse.from(inquiries.listForAdmin(status, cursor, size));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getAdminGroupInquiry")
    public AdminGroupInquiryResponse detail(@PathVariable Long id) {
        return AdminGroupInquiryResponse.from(inquiries.detailForAdmin(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createAdminGroupInquiry")
    public AdminGroupInquiryResponse create(@AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody GroupInquiryRequest request) {
        return AdminGroupInquiryResponse.from(inquiries.createExternal(admin.auditActorId(), request.toDetails()));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateAdminGroupInquiry")
    public AdminGroupInquiryResponse update(@PathVariable Long id, @AuthenticationPrincipal AdminPrincipal admin,
            @Valid @RequestBody GroupInquiryUpdateRequest request) {
        return AdminGroupInquiryResponse.from(inquiries.update(id, request.version(), request.status(), request.note(), admin.auditActorId()));
    }
}
