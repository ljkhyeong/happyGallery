package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.BookingCalendarResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.BookingCalendarSettingsResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.BookingTimeBlockResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateBookingTimeBlockRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.SlotResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateBookingCalendarDayRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateBookingCalendarSettingsRequest;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.CreateTimeBlockCommand;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.UpdateDayCommand;
import com.personal.happygallery.application.booking.port.in.BookingCalendarUseCase.UpdateSettingsCommand;
import com.personal.happygallery.application.booking.port.in.SlotManagementUseCase;
import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.domain.booking.Slot;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/slots")
public class AdminSlotController {

    private final SlotManagementUseCase slotManagementUseCase;
    private final SlotQueryUseCase slotQueryUseCase;
    private final BookingCalendarUseCase bookingCalendarUseCase;

    public AdminSlotController(SlotManagementUseCase slotManagementUseCase,
                               SlotQueryUseCase slotQueryUseCase,
                               BookingCalendarUseCase bookingCalendarUseCase) {
        this.slotManagementUseCase = slotManagementUseCase;
        this.slotQueryUseCase = slotQueryUseCase;
        this.bookingCalendarUseCase = bookingCalendarUseCase;
    }

    /** GET /api/v1/admin/slots?classId= — 클래스별 슬롯 전체 조회 (활성/비활성 포함) */
    @GetMapping
    public List<SlotResponse> listSlots(@RequestParam Long classId) {
        return slotQueryUseCase.listByClass(classId).stream()
                .map(SlotResponse::from)
                .toList();
    }

    /** PATCH /api/v1/admin/slots/{id}/deactivate — 슬롯 비활성화 */
    @PatchMapping("/{id}/deactivate")
    public SlotResponse deactivateSlot(@PathVariable Long id) {
        Slot slot = slotManagementUseCase.deactivateSlot(id);
        return SlotResponse.from(slot);
    }

    /** PATCH /api/v1/admin/slots/{id}/activate — 슬롯 관리자 활성 상태 복구 */
    @PatchMapping("/{id}/activate")
    public SlotResponse activateSlot(@PathVariable Long id) {
        Slot slot = slotManagementUseCase.activateSlot(id);
        return SlotResponse.from(slot);
    }

    @Operation(operationId = "getAdminBookingCalendar")
    @GetMapping("/calendar")
    public BookingCalendarResponse getCalendar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return BookingCalendarResponse.from(
                bookingCalendarUseCase.getCalendar(dateFrom, dateTo));
    }

    @Operation(operationId = "updateAdminBookingCalendarSettings")
    @PatchMapping("/calendar/settings")
    public BookingCalendarSettingsResponse updateCalendarSettings(
            @RequestBody @Valid UpdateBookingCalendarSettingsRequest request) {
        return BookingCalendarSettingsResponse.from(bookingCalendarUseCase.updateSettings(
                new UpdateSettingsCommand(
                        request.expectedVersion(),
                        request.openTime(),
                        request.closeTime(),
                        request.slotIntervalMin(),
                        request.blockPublicHolidays())));
    }

    @Operation(operationId = "updateAdminBookingCalendarDay")
    @PutMapping("/calendar/days/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCalendarDay(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody @Valid UpdateBookingCalendarDayRequest request) {
        bookingCalendarUseCase.updateDay(new UpdateDayCommand(date, request.mode(), request.reason()));
    }

    @Operation(operationId = "createAdminBookingTimeBlock")
    @PostMapping("/calendar/time-blocks")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingTimeBlockResponse createTimeBlock(
            @RequestBody @Valid CreateBookingTimeBlockRequest request) {
        return BookingTimeBlockResponse.from(bookingCalendarUseCase.createTimeBlock(
                new CreateTimeBlockCommand(
                        request.date(), request.startTime(), request.endTime(), request.reason())));
    }

    @Operation(operationId = "deleteAdminBookingTimeBlock")
    @DeleteMapping("/calendar/time-blocks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTimeBlock(@PathVariable Long id) {
        bookingCalendarUseCase.deleteTimeBlock(id);
    }
}
