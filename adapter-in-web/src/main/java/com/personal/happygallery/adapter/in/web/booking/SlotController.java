package com.personal.happygallery.adapter.in.web.booking;

import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.adapter.in.web.booking.dto.PublicSlotResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/slots", "/slots"})
public class SlotController {

    private final SlotQueryUseCase slotQueryUseCase;

    public SlotController(SlotQueryUseCase slotQueryUseCase) {
        this.slotQueryUseCase = slotQueryUseCase;
    }

    /** GET /slots?classId={}&date={} — 예약 가능 슬롯 목록 */
    @GetMapping
    public List<PublicSlotResponse> listAvailableSlots(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotQueryUseCase.listAvailable(classId, date).stream()
                .map(PublicSlotResponse::from)
                .toList();
    }
}
