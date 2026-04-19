package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateSlotRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SlotResponse;
import com.personal.happygallery.domain.booking.Slot;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/slots", "/admin/slots"})
public class AdminSlotController {

    private final SlotManagementUseCase slotManagementUseCase;
    private final SlotQueryUseCase slotQueryUseCase;

    public AdminSlotController(SlotManagementUseCase slotManagementUseCase,
                               SlotQueryUseCase slotQueryUseCase) {
        this.slotManagementUseCase = slotManagementUseCase;
        this.slotQueryUseCase = slotQueryUseCase;
    }

    /** GET /admin/slots?classId= — 클래스별 슬롯 전체 조회 (활성/비활성 포함) */
    @GetMapping
    public List<SlotResponse> listSlots(@RequestParam Long classId) {
        return slotQueryUseCase.listByClass(classId).stream()
                .map(SlotResponse::from)
                .toList();
    }

    /** POST /admin/slots — 슬롯 생성 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SlotResponse createSlot(@RequestBody @Valid CreateSlotRequest request) {
        Slot slot = slotManagementUseCase.createSlot(
                request.classId(), request.startAt(), request.endAt());
        return SlotResponse.from(slot);
    }

    /** PATCH /admin/slots/{id}/deactivate — 슬롯 비활성화 */
    @PatchMapping("/{id}/deactivate")
    public SlotResponse deactivateSlot(@PathVariable Long id) {
        Slot slot = slotManagementUseCase.deactivateSlot(id);
        return SlotResponse.from(slot);
    }
}
