package com.personal.happygallery.adapter.in.web.monitoring;

import com.personal.happygallery.application.monitoring.port.in.ClientMonitoringUseCase;
import com.personal.happygallery.adapter.in.web.monitoring.dto.CaptureClientEventRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/monitoring/client-events")
public class ClientMonitoringController {

    private final ClientMonitoringUseCase clientMonitoringUseCase;

    public ClientMonitoringController(ClientMonitoringUseCase clientMonitoringUseCase) {
        this.clientMonitoringUseCase = clientMonitoringUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "captureClientEvent")
    public void capture(@RequestBody @Valid CaptureClientEventRequest request,
                        @AuthenticationPrincipal CustomerPrincipal customer) {
        clientMonitoringUseCase.captureFrontendEvent(
                request.event(),
                request.path(),
                request.source(),
                request.target(),
                customer != null ? customer.userId() : null);
    }
}
