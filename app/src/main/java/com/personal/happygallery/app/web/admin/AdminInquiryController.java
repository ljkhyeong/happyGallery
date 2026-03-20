package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.app.inquiry.port.in.InquiryUseCase.InquiryWithUser;
import com.personal.happygallery.app.web.AdminAuthFilter;
import com.personal.happygallery.app.web.admin.dto.AdminInquiryResponse;
import com.personal.happygallery.app.web.admin.dto.InquiryReplyRequest;
import com.personal.happygallery.domain.inquiry.Inquiry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/inquiries", "/admin/inquiries"})
public class AdminInquiryController {

    private final InquiryUseCase inquiryUseCase;

    public AdminInquiryController(InquiryUseCase inquiryUseCase) {
        this.inquiryUseCase = inquiryUseCase;
    }

    @GetMapping
    public List<AdminInquiryResponse> list() {
        return inquiryUseCase.listAll().stream()
                .map(AdminInquiryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public AdminInquiryResponse detail(@PathVariable Long id) {
        return AdminInquiryResponse.from(inquiryUseCase.findByIdForAdmin(id));
    }

    @PostMapping("/{id}/reply")
    public AdminInquiryResponse reply(@PathVariable Long id,
                                      @RequestBody @Valid InquiryReplyRequest request,
                                      HttpServletRequest httpRequest) {
        Long adminId = (Long) httpRequest.getAttribute(AdminAuthFilter.ADMIN_USER_ID_ATTR);
        Inquiry inquiry = inquiryUseCase.reply(id, request.replyContent(), adminId);
        String userName = inquiryUseCase.findByIdForAdmin(id).userName();
        return AdminInquiryResponse.from(new InquiryWithUser(inquiry, userName));
    }
}
