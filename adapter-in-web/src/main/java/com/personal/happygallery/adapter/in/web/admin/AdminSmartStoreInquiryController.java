package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreCustomerInquiryResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreInquiryAnswerRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreInquiryResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreInquiryAnswerTemplateResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreInquiryPageResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreCustomerInquiryPageResponse;
import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/smartstore-inquiries")
public class AdminSmartStoreInquiryController {

    private final SmartStoreInquiryUseCase useCase;

    public AdminSmartStoreInquiryController(SmartStoreInquiryUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(operationId = "listSmartStoreInquiries")
    public List<SmartStoreInquiryResponse> list(
            @RequestParam(defaultValue = "true") boolean unansweredOnly,
            @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit) {
        return useCase.list(unansweredOnly, limit).stream()
                .map(SmartStoreInquiryResponse::from)
                .toList();
    }

    @GetMapping("/template")
    @Operation(operationId = "getSmartStoreInquiryAnswerTemplate")
    public SmartStoreInquiryAnswerTemplateResponse getAnswerTemplate() {
        return SmartStoreInquiryAnswerTemplateResponse.from(
                useCase.getProductInquiryAnswerTemplate());
    }

    @GetMapping("/page")
    @Operation(operationId = "listSmartStoreInquiriesPage")
    public SmartStoreInquiryPageResponse listPage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "true") boolean unansweredOnly,
            @RequestParam(defaultValue = "0") @Min(0) @Max(999999) int page,
            @RequestParam(defaultValue = "50") @Min(10) @Max(100) int size) {
        return SmartStoreInquiryPageResponse.from(useCase.listPage(from, to, unansweredOnly, page, size));
    }

    @GetMapping("/customers/page")
    @Operation(operationId = "listSmartStoreCustomerInquiriesPage")
    public SmartStoreCustomerInquiryPageResponse listCustomerPage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "true") boolean unansweredOnly,
            @RequestParam(defaultValue = "0") @Min(0) @Max(999999) int page,
            @RequestParam(defaultValue = "50") @Min(10) @Max(100) int size) {
        return SmartStoreCustomerInquiryPageResponse.from(useCase.listCustomerPage(from, to, unansweredOnly, page, size));
    }

    @PutMapping("/{questionId}/answer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "answerSmartStoreInquiry")
    public void answer(
            @PathVariable long questionId,
            @Valid @RequestBody SmartStoreInquiryAnswerRequest request) {
        useCase.answer(questionId, request.content());
    }

    @GetMapping("/customers")
    @Operation(operationId = "listSmartStoreCustomerInquiries")
    public List<SmartStoreCustomerInquiryResponse> listCustomerInquiries(
            @RequestParam(defaultValue = "true") boolean unansweredOnly,
            @RequestParam(defaultValue = "100") @Min(1) @Max(200) int limit) {
        return useCase.listCustomerInquiries(unansweredOnly, limit).stream()
                .map(SmartStoreCustomerInquiryResponse::from)
                .toList();
    }

    @PutMapping("/customers/{inquiryNo}/answer")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "answerSmartStoreCustomerInquiry")
    public void answerCustomerInquiry(
            @PathVariable long inquiryNo,
            @Valid @RequestBody SmartStoreInquiryAnswerRequest request) {
        useCase.answerCustomerInquiry(inquiryNo, request.content());
    }

    @PutMapping("/customers/{inquiryNo}/answer/{answerContentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "updateSmartStoreCustomerInquiryAnswer")
    public void updateCustomerInquiryAnswer(
            @PathVariable @Min(1) long inquiryNo,
            @PathVariable @Min(1) long answerContentId,
            @Valid @RequestBody SmartStoreInquiryAnswerRequest request) {
        useCase.updateCustomerInquiryAnswer(inquiryNo, answerContentId, request.content());
    }
}
