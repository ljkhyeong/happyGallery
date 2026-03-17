package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.inquiry.InquiryService;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.domain.inquiry.Inquiry;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/inquiries")
public class MeInquiryController {

    private final InquiryService inquiryService;

    public MeInquiryController(InquiryService inquiryService) {
        this.inquiryService = inquiryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse create(@RequestBody @Valid CreateInquiryRequest request,
                                  HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        Inquiry inquiry = inquiryService.create(userId, request.title(), request.content());
        return InquiryResponse.from(inquiry);
    }

    @GetMapping
    public List<InquiryResponse> list(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return inquiryService.listByUser(userId).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public InquiryResponse detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return InquiryResponse.from(inquiryService.findByIdAndUser(id, userId));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }

    // ── DTO ──

    public record CreateInquiryRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content
    ) {}

    public record InquiryResponse(
            Long id, String title, String content,
            boolean hasReply, String replyContent, LocalDateTime repliedAt,
            LocalDateTime createdAt
    ) {
        static InquiryResponse from(Inquiry i) {
            return new InquiryResponse(
                    i.getId(), i.getTitle(), i.getContent(),
                    i.hasReply(), i.getReplyContent(), i.getRepliedAt(),
                    i.getCreatedAt());
        }
    }
}
