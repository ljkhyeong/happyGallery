package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.AdminBookingCancelRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminBookingCancelResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.AdminBookingResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.BookingCancellationTaskCompletionResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.BookingCancellationTaskResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.BookingNoShowResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.BookingSettlementResponse;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateBookingArrearsRequest;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase;
import com.personal.happygallery.application.booking.port.in.AdminBookingCancelUseCase.AdminCancelCommand;
import com.personal.happygallery.application.booking.port.in.AdminBookingQueryUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase;
import com.personal.happygallery.application.booking.port.in.BookingNoShowUseCase;
import com.personal.happygallery.application.booking.port.in.BookingSettlementUseCase;
import com.personal.happygallery.application.search.dto.AdminBookingSearchRow;
import com.personal.happygallery.application.search.port.in.AdminBookingSearchUseCase;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingStatus;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingController {

    private final AdminBookingQueryUseCase adminBookingQueryUseCase;
    private final AdminBookingSearchUseCase adminBookingSearchUseCase;
    private final BookingNoShowUseCase bookingNoShowUseCase;
    private final BookingSettlementUseCase bookingSettlementUseCase;
    private final AdminBookingCancelUseCase adminBookingCancelUseCase;
    private final BookingCancellationTaskUseCase bookingCancellationTaskUseCase;

    public AdminBookingController(AdminBookingQueryUseCase adminBookingQueryUseCase,
                                  AdminBookingSearchUseCase adminBookingSearchUseCase,
                                  BookingNoShowUseCase bookingNoShowUseCase,
                                  BookingSettlementUseCase bookingSettlementUseCase,
                                  AdminBookingCancelUseCase adminBookingCancelUseCase,
                                  BookingCancellationTaskUseCase bookingCancellationTaskUseCase) {
        this.adminBookingQueryUseCase = adminBookingQueryUseCase;
        this.adminBookingSearchUseCase = adminBookingSearchUseCase;
        this.bookingNoShowUseCase = bookingNoShowUseCase;
        this.bookingSettlementUseCase = bookingSettlementUseCase;
        this.adminBookingCancelUseCase = adminBookingCancelUseCase;
        this.bookingCancellationTaskUseCase = bookingCancellationTaskUseCase;
    }

    /** GET /api/v1/admin/bookings?date=2026-03-08&status=BOOKED — 날짜별 예약 조회 (상태 필터 선택) */
    @GetMapping
    @Operation(operationId = "listBookings")
    public List<AdminBookingResponse> listBookings(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) BookingStatus status) {
        return adminBookingQueryUseCase.listBookings(date, status).stream()
                .map(AdminBookingResponse::from)
                .toList();
    }

    /** GET /api/v1/admin/bookings/search — 상태·날짜·키워드 기반 예약 검색 (OFFSET + 지연 조인) */
    @GetMapping("/search")
    @Operation(operationId = "searchBookings")
    public OffsetPage<AdminBookingSearchRow> searchBookings(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminBookingSearchUseCase.search(status, dateFrom, dateTo, keyword, page, size);
    }

    /** 결석 처리 — 8회권 크레딧 소멸 유지, 상태 NO_SHOW 전이 */
    @PostMapping("/{bookingId}/no-show")
    @Operation(operationId = "markNoShow")
    public BookingNoShowResponse markNoShow(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        Booking booking = bookingNoShowUseCase.markNoShow(bookingId, admin.adminUserId());
        return BookingNoShowResponse.from(booking);
    }

    /** 현장 잔금 결제 완료 처리. */
    @PostMapping("/{bookingId}/balance-payment")
    @Operation(operationId = "markBalancePaid")
    public BookingSettlementResponse markBalancePaid(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return BookingSettlementResponse.from(
                bookingSettlementUseCase.markBalancePaid(bookingId, admin.adminUserId()));
    }

    /** 미수 여부를 명시적으로 설정한다. */
    @PutMapping("/{bookingId}/arrears")
    @Operation(operationId = "updateArrears")
    public BookingSettlementResponse updateArrears(
            @PathVariable Long bookingId,
            @RequestBody @Valid UpdateBookingArrearsRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return BookingSettlementResponse.from(
                bookingSettlementUseCase.updateArrears(bookingId, request.arrears(), admin.adminUserId()));
    }

    /** 수업이 끝난 BOOKED 예약을 완료한다. */
    @PostMapping("/{bookingId}/complete")
    @Operation(operationId = "complete")
    public BookingSettlementResponse complete(
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return BookingSettlementResponse.from(
                bookingSettlementUseCase.complete(bookingId, admin.adminUserId()));
    }

    /** 공방 사정으로 예약을 취소하고 예약금 환불 또는 8회권 복구를 시작한다. */
    @PostMapping("/{bookingId}/cancel")
    @Operation(operationId = "cancelAdminBooking")
    public AdminBookingCancelResponse cancel(
            @PathVariable Long bookingId,
            @RequestBody @Valid AdminBookingCancelRequest request,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return AdminBookingCancelResponse.from(adminBookingCancelUseCase.cancel(
                new AdminCancelCommand(bookingId, admin.adminUserId(), request.reason())));
    }

    /** 공방 사정 취소 뒤 운영자가 직접 마무리해야 하는 작업을 조회한다. */
    @GetMapping("/cancellation-tasks")
    @Operation(operationId = "listPendingBookingCancellationTasks")
    public List<BookingCancellationTaskResponse> listPendingCancellationTasks() {
        return bookingCancellationTaskUseCase.listPending().stream()
                .map(BookingCancellationTaskResponse::from)
                .toList();
    }

    /** 예약 취소 후속 작업을 완료한다. 이미 완료된 작업은 동일한 완료 상태를 반환한다. */
    @PostMapping("/cancellation-tasks/{taskId}/complete")
    @Operation(operationId = "completeBookingCancellationTask")
    public BookingCancellationTaskCompletionResponse completeCancellationTask(
            @PathVariable Long taskId,
            @AuthenticationPrincipal AdminPrincipal admin) {
        return BookingCancellationTaskCompletionResponse.from(
                bookingCancellationTaskUseCase.complete(taskId, admin.adminUserId()));
    }
}
