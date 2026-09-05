package com.personal.happygallery.adapter.in.web.inquiry;

import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryRequest;
import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryReceiptResponse;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupInquiryController {
    private final GroupInquiryUseCase inquiries;
    public GroupInquiryController(GroupInquiryUseCase inquiries) { this.inquiries = inquiries; }

    @PostMapping("/api/v1/group-inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createGuestGroupInquiry")
    public GroupInquiryReceiptResponse create(@Valid @RequestBody GroupInquiryRequest request) {
        return GroupInquiryReceiptResponse.from(inquiries.create(null, request.toDetails()));
    }
}
