package com.personal.happygallery.app.web.booking;

import com.personal.happygallery.app.booking.SlotQueryService;
import com.personal.happygallery.app.web.booking.dto.PublicSlotResponse;
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

    private final SlotQueryService slotQueryService;

    public SlotController(SlotQueryService slotQueryService) {
        this.slotQueryService = slotQueryService;
    }

    /** GET /slots?classId={}&date={} — 예약 가능 슬롯 목록 */
    @GetMapping
    public List<PublicSlotResponse> listAvailableSlots(
            @RequestParam Long classId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotQueryService.listAvailable(classId, date).stream()
                .map(PublicSlotResponse::from)
                .toList();
    }
}
