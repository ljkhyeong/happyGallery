package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.adapter.in.web.customer.dto.CreateInquiryRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.InquiryResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    @Operation(operationId = "createMyInquiry")
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse create(@RequestBody @Valid CreateInquiryRequest request,
                                  @AuthenticationPrincipal CustomerPrincipal customer) {
        var inquiry = inquiryUseCase.create(customer.userId(), request.title(), request.content());
        return InquiryResponse.from(inquiry);
    }

    @GetMapping
    @Operation(operationId = "listMyInquiries")
    public List<InquiryResponse> list(@AuthenticationPrincipal CustomerPrincipal customer) {
        return inquiryUseCase.listByUser(customer.userId()).stream()
                .map(InquiryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getMyInquiry")
    public InquiryResponse detail(@PathVariable Long id,
                                  @AuthenticationPrincipal CustomerPrincipal customer) {
        var inquiry = inquiryUseCase.findByIdAndUser(id, customer.userId());
        return InquiryResponse.from(inquiry);
    }
}
