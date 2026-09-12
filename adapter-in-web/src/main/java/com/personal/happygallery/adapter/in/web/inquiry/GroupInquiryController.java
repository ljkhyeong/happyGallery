package com.personal.happygallery.adapter.in.web.inquiry;

import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryRequest;
import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryReceiptResponse;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.application.security.port.in.BotProtectionUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupInquiryController {
    private final GroupInquiryUseCase inquiries;
    private final BotProtectionUseCase botProtection;
    public GroupInquiryController(GroupInquiryUseCase inquiries, BotProtectionUseCase botProtection) {
        this.inquiries = inquiries;
        this.botProtection = botProtection;
    }

    @PostMapping("/api/v1/group-inquiries")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createGuestGroupInquiry")
    public GroupInquiryReceiptResponse create(
            @RequestHeader(value = "X-Bot-Token", required = false) String botToken,
            @Valid @RequestBody GroupInquiryRequest request) {
        botProtection.verify(botToken, "group_inquiry");
        return GroupInquiryReceiptResponse.from(inquiries.create(null, request.toDetails()));
    }
}
