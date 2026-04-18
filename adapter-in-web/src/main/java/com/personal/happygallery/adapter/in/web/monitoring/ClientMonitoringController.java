package com.personal.happygallery.adapter.in.web.monitoring;

import com.personal.happygallery.application.monitoring.port.in.ClientMonitoringUseCase;
import com.personal.happygallery.adapter.in.web.monitoring.dto.CaptureClientEventRequest;
import com.personal.happygallery.adapter.in.web.resolver.CustomerUserId;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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
    public void capture(@RequestBody @Valid CaptureClientEventRequest request,
                        @CustomerUserId Long userId) {
        clientMonitoringUseCase.captureFrontendEvent(
                request.event(),
                request.path(),
                request.source(),
                request.target(),
                userId);
    }
}
