package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.CreateEventRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateEventRequest;
import com.personal.happygallery.adapter.in.web.event.dto.EventResponse;
import com.personal.happygallery.application.event.port.in.EventAdminUseCase;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
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
@RequestMapping("/api/v1/admin/events")
public class AdminEventController {

    private final EventAdminUseCase eventAdminUseCase;

    public AdminEventController(EventAdminUseCase eventAdminUseCase) {
        this.eventAdminUseCase = eventAdminUseCase;
    }

    @GetMapping
    @Operation(operationId = "listAdminEvents")
    public List<EventResponse> list() {
        return eventAdminUseCase.listAll().stream()
                .map(EventResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getAdminEvent")
    public EventResponse get(@PathVariable Long id) {
        return EventResponse.from(eventAdminUseCase.getForEdit(id));
    }

    @PostMapping
    @Operation(operationId = "createAdminEvent")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@RequestBody @Valid CreateEventRequest request) {
        return EventResponse.from(eventAdminUseCase.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    @Operation(operationId = "updateAdminEvent")
    public EventResponse update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateEventRequest request
    ) {
        return EventResponse.from(eventAdminUseCase.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @Operation(operationId = "deleteAdminEvent")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @RequestParam @PositiveOrZero long expectedVersion
    ) {
        eventAdminUseCase.delete(id, expectedVersion);
    }
}
