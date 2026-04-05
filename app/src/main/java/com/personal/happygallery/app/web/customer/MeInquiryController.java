package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.app.web.customer.dto.CreateInquiryRequest;
import com.personal.happygallery.app.web.customer.dto.InquiryResponse;
import com.personal.happygallery.app.web.resolver.CustomerUserId;
import jakarta.validation.Valid;
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

    private final InquiryUseCase inquiryUseCase;

    public MeInquiryController(InquiryUseCase inquiryUseCase) {
        this.inquiryUseCase = inquiryUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse create(@RequestBody @Valid CreateInquiryRequest request,
                                  @CustomerUserId Long userId) {
        var inquiry = inquiryUseCase.create(userId, request.title(), request.content());
        return InquiryResponse.from(inquiry);
    }

    @GetMapping
    public List<InquiryResponse> list(@CustomerUserId Long userId) {
        return inquiryUseCase.listByUser(userId).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public InquiryResponse detail(@PathVariable Long id, @CustomerUserId Long userId) {
        var inquiry = inquiryUseCase.findByIdAndUser(id, userId);
        return InquiryResponse.from(inquiry);
    }
}
