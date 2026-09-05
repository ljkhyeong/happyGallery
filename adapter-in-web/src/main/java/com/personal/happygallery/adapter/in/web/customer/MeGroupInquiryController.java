package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.MyGroupInquiryResponse;
import com.personal.happygallery.adapter.in.web.customer.dto.UpdateMyGroupInquiryRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CancelMyGroupInquiryRequest;
import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryReceiptResponse;
import com.personal.happygallery.adapter.in.web.inquiry.dto.GroupInquiryPageResponse;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me/group-inquiries")
public class MeGroupInquiryController {
    private final GroupInquiryUseCase inquiries;
    public MeGroupInquiryController(GroupInquiryUseCase inquiries) { this.inquiries = inquiries; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createMyGroupInquiry")
    public GroupInquiryReceiptResponse create(@AuthenticationPrincipal CustomerPrincipal customer,
            @Valid @RequestBody GroupInquiryRequest request) {
        return GroupInquiryReceiptResponse.from(inquiries.create(customer.userId(), request.toDetails()));
    }

    @GetMapping
    @Operation(operationId = "listMyGroupInquiries")
    public GroupInquiryPageResponse list(@AuthenticationPrincipal CustomerPrincipal customer,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int size) {
        return GroupInquiryPageResponse.from(inquiries.listForMember(customer.userId(), cursor, size));
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getMyGroupInquiry")
    public MyGroupInquiryResponse detail(@AuthenticationPrincipal CustomerPrincipal customer, @PathVariable Long id) {
        return MyGroupInquiryResponse.from(inquiries.detailForMember(customer.userId(), id));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateMyGroupInquiry")
    public MyGroupInquiryResponse update(@AuthenticationPrincipal CustomerPrincipal customer, @PathVariable Long id,
            @Valid @RequestBody UpdateMyGroupInquiryRequest request) {
        return MyGroupInquiryResponse.from(inquiries.reviseByMember(customer.userId(), id, request.version(),
                request.headcount(), request.preferredSchedule()));
    }

    @PostMapping("/{id}/cancel")
    @Operation(operationId = "cancelMyGroupInquiry")
    public MyGroupInquiryResponse cancel(@AuthenticationPrincipal CustomerPrincipal customer, @PathVariable Long id,
            @Valid @RequestBody CancelMyGroupInquiryRequest request) {
        return MyGroupInquiryResponse.from(inquiries.cancelByMember(customer.userId(), id, request.version()));
    }
}
