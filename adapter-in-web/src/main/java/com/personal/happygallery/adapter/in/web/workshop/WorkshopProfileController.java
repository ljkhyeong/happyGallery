package com.personal.happygallery.adapter.in.web.workshop;

import com.personal.happygallery.adapter.in.web.workshop.dto.WorkshopProfileResponse;
import com.personal.happygallery.application.store.port.in.WorkshopProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workshop")
public class WorkshopProfileController {

    private final WorkshopProfileUseCase useCase;

    public WorkshopProfileController(WorkshopProfileUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @Operation(operationId = "getWorkshopProfile")
    public WorkshopProfileResponse get() {
        return WorkshopProfileResponse.from(useCase.get());
    }
}
