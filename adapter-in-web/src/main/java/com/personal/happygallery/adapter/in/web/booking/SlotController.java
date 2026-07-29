package com.personal.happygallery.adapter.in.web.booking;

import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.adapter.in.web.booking.dto.PublicSlotResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/slots")
public class SlotController {

    private final SlotQueryUseCase slotQueryUseCase;

    public SlotController(SlotQueryUseCase slotQueryUseCase) {
        this.slotQueryUseCase = slotQueryUseCase;
    }

    /** GET /api/v1/slots?classId={}&date={} — 예약 가능 슬롯 목록 */
    @GetMapping
    @Operation(operationId = "listAvailableSlots")
    public List<PublicSlotResponse> listAvailableSlots(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotQueryUseCase.listAvailable(classId, date).stream()
                .map(PublicSlotResponse::from)
                .toList();
    }

    /** GET /api/v1/slots/upcoming?classId={}&days=14 — 향후 예약 가능 슬롯 탐색 */
    @GetMapping("/upcoming")
    @Operation(operationId = "listUpcomingSlots")
    public List<PublicSlotResponse> listUpcomingSlots(
            @RequestParam Long classId,
            @RequestParam(defaultValue = "14") @Min(1) @Max(30) int days) {
        return slotQueryUseCase.listUpcoming(classId, days).stream()
                .map(PublicSlotResponse::from)
                .toList();
    }
}
