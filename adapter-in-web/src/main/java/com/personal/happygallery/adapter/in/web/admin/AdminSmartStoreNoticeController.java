package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.ApplySmartStoreNoticeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SaveSmartStoreNoticeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreNoticeIdResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreNoticePageResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.SmartStoreNoticeResponse;
import com.personal.happygallery.application.product.port.in.SmartStoreProductNoticeUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/smartstore-notices")
public class AdminSmartStoreNoticeController {

    private final SmartStoreProductNoticeUseCase useCase;

    public AdminSmartStoreNoticeController(SmartStoreProductNoticeUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(operationId = "listSmartStoreProductNotices")
    public SmartStoreNoticePageResponse list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "100") @Min(1) @Max(100) int size) {
        return SmartStoreNoticePageResponse.from(useCase.list(page, size));
    }

    @GetMapping("/{sellerNoticeId}")
    @Operation(operationId = "getSmartStoreProductNotice")
    public SmartStoreNoticeResponse get(@PathVariable Long sellerNoticeId) {
        return SmartStoreNoticeResponse.from(useCase.get(sellerNoticeId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "createSmartStoreProductNotice")
    public SmartStoreNoticeIdResponse create(
            @Valid @RequestBody SaveSmartStoreNoticeRequest request) {
        return new SmartStoreNoticeIdResponse(useCase.create(request.toCommand()));
    }

    @PutMapping("/{sellerNoticeId}")
    @Operation(operationId = "updateSmartStoreProductNotice")
    public SmartStoreNoticeIdResponse update(
            @PathVariable Long sellerNoticeId,
            @Valid @RequestBody SaveSmartStoreNoticeRequest request) {
        return new SmartStoreNoticeIdResponse(
                useCase.update(sellerNoticeId, request.toCommand()));
    }

    @DeleteMapping("/{sellerNoticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteSmartStoreProductNotice")
    public void delete(@PathVariable Long sellerNoticeId) {
        useCase.delete(sellerNoticeId);
    }

    @PutMapping("/{sellerNoticeId}/products")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "applySmartStoreProductNotice")
    public void apply(
            @PathVariable Long sellerNoticeId,
            @Valid @RequestBody ApplySmartStoreNoticeRequest request) {
        useCase.apply(sellerNoticeId, request.channelProductNos());
    }
}
