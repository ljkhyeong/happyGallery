package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminSlotSessionCancelRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminSlotSessionCancelResponse;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.CancelSessionCommand;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/slots")
public class AdminSlotSessionController {

    private final AdminBookingCancelUseCase adminBookingCancelUseCase;

    public AdminSlotSessionController(AdminBookingCancelUseCase adminBookingCancelUseCase) {
        this.adminBookingCancelUseCase = adminBookingCancelUseCase;
    }

    /** 신규 접수를 중단한 슬롯의 BOOKED 예약을 모두 취소하고 고객 보상을 시작한다. */
    @PostMapping("/{slotId}/cancel-session")
    @Operation(operationId = "cancelAdminSlotSession")
    public AdminSlotSessionCancelResponse cancelSession(
            @PathVariable Long slotId,
            @RequestBody @Valid AdminSlotSessionCancelRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminSlotSessionCancelResponse.from(adminBookingCancelUseCase.cancelSession(
                new CancelSessionCommand(slotId, admin.adminUserId(), request.reason())));
    }
}
