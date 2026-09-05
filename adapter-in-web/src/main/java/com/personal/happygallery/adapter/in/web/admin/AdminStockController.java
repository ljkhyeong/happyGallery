package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.StockLevelResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateStockThresholdRequest;
import com.personal.happygallery.application.product.port.in.StockThresholdUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stock-levels")
public class AdminStockController {
    private final StockThresholdUseCase thresholds;

    public AdminStockController(StockThresholdUseCase thresholds) { this.thresholds = thresholds; }

    @GetMapping
    @Operation(operationId = "listAdminStockLevels")
    public List<StockLevelResponse> list(@RequestParam(required = false) Long productId) {
        return thresholds.list(productId).stream().map(StockLevelResponse::from).toList();
    }

    @PutMapping("/threshold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "updateAdminStockThreshold")
    public void update(@Valid @RequestBody UpdateStockThresholdRequest request) {
        thresholds.update(request.productId(), request.productVariantId(), request.minimumStock(), request.version());
    }
}
