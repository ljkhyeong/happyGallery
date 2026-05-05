package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.order.port.in.AdminOrderQueryUseCase;
import com.personal.happygallery.application.order.port.in.AdminOrderResponse;
import com.personal.happygallery.application.order.port.in.OrderHistoryResponse;
import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.application.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/admin/orders", "/admin/orders"})
public class AdminOrderQueryController {

    private final AdminOrderQueryUseCase adminOrderQueryUseCase;
    private final AdminOrderSearchUseCase adminOrderSearchUseCase;

    public AdminOrderQueryController(AdminOrderQueryUseCase adminOrderQueryUseCase,
                                     AdminOrderSearchUseCase adminOrderSearchUseCase) {
        this.adminOrderQueryUseCase = adminOrderQueryUseCase;
        this.adminOrderSearchUseCase = adminOrderSearchUseCase;
    }

    /** GET /admin/orders?status=...&cursor=...&size=20 — 커서 기반 주문 목록 조회 */
    @GetMapping
    public CursorPage<AdminOrderResponse> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderQueryUseCase.listOrders(status, cursor, size);
    }

    /** GET /admin/orders/search — 상태·날짜·키워드 기반 주문 검색 (OFFSET + 지연 조인) */
    @GetMapping("/search")
    public OffsetPage<AdminOrderSearchRow> searchOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminOrderSearchUseCase.search(status, dateFrom, dateTo, keyword, page, size);
    }

    /** GET /admin/orders/{id}/history — 주문 결정 이력 조회 */
    @GetMapping("/{id}/history")
    public List<OrderHistoryResponse> getOrderHistory(@PathVariable Long id) {
        return adminOrderQueryUseCase.getOrderHistory(id);
    }
}
