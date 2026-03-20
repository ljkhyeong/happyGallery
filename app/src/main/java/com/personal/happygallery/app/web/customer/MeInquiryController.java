package com.personal.happygallery.app.web.customer;

import com.personal.happygallery.app.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.customer.dto.CreateInquiryRequest;
import com.personal.happygallery.app.web.customer.dto.InquiryResponse;
import jakarta.servlet.http.HttpServletRequest;
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
                                  HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return InquiryResponse.from(inquiryUseCase.create(userId, request.title(), request.content()));
    }

    @GetMapping
    public List<InquiryResponse> list(HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return inquiryUseCase.listByUser(userId).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public InquiryResponse detail(@PathVariable Long id, HttpServletRequest httpRequest) {
        Long userId = getUserId(httpRequest);
        return InquiryResponse.from(inquiryUseCase.findByIdAndUser(id, userId));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}
