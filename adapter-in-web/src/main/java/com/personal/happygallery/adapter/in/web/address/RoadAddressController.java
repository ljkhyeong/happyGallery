package com.personal.happygallery.adapter.in.web.address;

import com.personal.happygallery.adapter.in.web.address.dto.RoadAddressResponse;
import com.personal.happygallery.application.address.port.in.RoadAddressSearchUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/addresses")
public class RoadAddressController {

    private final RoadAddressSearchUseCase useCase;

    public RoadAddressController(RoadAddressSearchUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/search")
    @Operation(operationId = "searchRoadAddresses")
    public List<RoadAddressResponse> search(
            @RequestParam @NotBlank @Size(min = 2, max = 100) String keyword) {
        return useCase.search(keyword).stream()
                .map(RoadAddressResponse::from)
                .toList();
    }
}
