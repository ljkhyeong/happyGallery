package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.app.booking.SlotManagementService;
import com.personal.happygallery.app.web.admin.dto.CreateSlotRequest;
import com.personal.happygallery.app.web.admin.dto.SlotResponse;
import com.personal.happygallery.domain.booking.Slot;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/slots")
public class AdminSlotController {

    private final SlotManagementService slotManagementService;

    public AdminSlotController(SlotManagementService slotManagementService) {
        this.slotManagementService = slotManagementService;
    }

    /** POST /admin/slots — 슬롯 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse createSlot(@RequestBody @Valid CreateSlotRequest request) {
        Slot slot = slotManagementService.createSlot(
                request.classId(), request.startAt(), request.endAt());
        return SlotResponse.from(slot);
    }

    /** PATCH /admin/slots/{id}/deactivate — 슬롯 비활성화 */
    @PatchMapping("/{id}/deactivate")
    public SlotResponse deactivateSlot(@PathVariable Long id) {
        Slot slot = slotManagementService.deactivateSlot(id);
        return SlotResponse.from(slot);
    }
}
