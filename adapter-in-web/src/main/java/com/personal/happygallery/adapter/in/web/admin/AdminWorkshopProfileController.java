package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.UpdateWorkshopProfileRequest;
import com.personal.happygallery.adapter.in.web.workshop.dto.WorkshopProfileResponse;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase.UpdateCommand;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/workshop")
public class AdminWorkshopProfileController {

    private final WorkshopProfileUseCase useCase;

    public AdminWorkshopProfileController(WorkshopProfileUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(operationId = "getAdminWorkshopProfile")
    public WorkshopProfileResponse get() {
        return WorkshopProfileResponse.from(useCase.get());
    }

    @PutMapping
    @Operation(operationId = "updateAdminWorkshopProfile")
    public WorkshopProfileResponse update(@RequestBody @Valid UpdateWorkshopProfileRequest request) {
        return WorkshopProfileResponse.from(useCase.update(new UpdateCommand(
                request.name(), request.phone(), request.postalCode(),
                request.addressLine1(), request.addressLine2(), request.businessHours(),
                request.mapUrl(), request.parkingInfo(), request.businessRegistrationNumber(),
                request.representativeName(), request.email(), request.mailOrderRegistrationNumber(),
                request.introduction(), request.kakaoTalkId(),
                request.naverTalkUrl(), request.naverBlogUrl(),
                request.instagramUrl(), request.smartStoreUrl())));
    }
}
