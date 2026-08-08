package com.personal.happygallery.adapter.in.web.event;

import com.personal.happygallery.adapter.in.web.event.dto.EventResponse;
import com.personal.happygallery.application.event.port.in.EventQueryUseCase;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events")
public class EventController {

    private final EventQueryUseCase eventQueryUseCase;

    public EventController(EventQueryUseCase eventQueryUseCase) {
        this.eventQueryUseCase = eventQueryUseCase;
    }

    @GetMapping
    @Operation(operationId = "listPublicEvents")
    public ResponseEntity<List<EventResponse>> list() {
        List<EventResponse> events = eventQueryUseCase.listPublicEvents().stream()
                .map(EventResponse::from)
                .toList();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(events);
    }

    @GetMapping("/{id}")
    @Operation(operationId = "getPublicEvent")
    public ResponseEntity<EventResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(EventResponse.from(eventQueryUseCase.getPublicEvent(id)));
    }
}
