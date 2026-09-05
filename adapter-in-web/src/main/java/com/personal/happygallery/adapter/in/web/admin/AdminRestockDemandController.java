package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.RestockDemandPageResponse;
import com.personal.happygallery.application.product.port.in.RestockDemandUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/restock-demand")
public class AdminRestockDemandController {
    private final RestockDemandUseCase demand;
    public AdminRestockDemandController(RestockDemandUseCase demand) { this.demand = demand; }
    @GetMapping
    @Operation(operationId = "listAdminRestockDemand")
    public RestockDemandPageResponse list(@RequestParam(required = false) @Positive Long productId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return RestockDemandPageResponse.from(demand.list(productId, page, size));
    }
}
