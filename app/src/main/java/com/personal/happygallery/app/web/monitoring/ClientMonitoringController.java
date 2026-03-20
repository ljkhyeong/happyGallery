package com.personal.happygallery.app.web.monitoring;

import com.personal.happygallery.app.monitoring.port.in.ClientMonitoringUseCase;
import com.personal.happygallery.app.web.CustomerAuthFilter;
import com.personal.happygallery.app.web.monitoring.dto.CaptureClientEventRequest;
import jakarta.servlet.http.HttpServletRequest;
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
                        HttpServletRequest servletRequest) {
        clientMonitoringUseCase.captureFrontendEvent(
                request.event(),
                request.path(),
                request.source(),
                request.target(),
                getUserId(servletRequest));
    }

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute(CustomerAuthFilter.CUSTOMER_USER_ID_ATTR);
    }
}
