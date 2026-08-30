package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminOrderClaimController;
import com.personal.happygallery.adapter.in.web.customer.MeOrderClaimController;
import com.personal.happygallery.adapter.in.web.order.GuestOrderClaimController;
import com.personal.happygallery.application.order.port.in.AdminOrderClaimUseCase;
import com.personal.happygallery.application.order.port.in.OrderClaimUseCase;
import com.personal.happygallery.application.order.port.in.OrderClaimView;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.order.OrderClaimResolution;
import com.personal.happygallery.domain.order.OrderClaimStatus;
import com.personal.happygallery.domain.order.OrderClaimType;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderClaimApiRestDocsTest extends RestDocsTestSupport {

    private MockMvc mockMvc;
    private OrderClaimUseCase orderClaimUseCase;
    private AdminOrderClaimUseCase adminOrderClaimUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        orderClaimUseCase = mock(OrderClaimUseCase.class);
        adminOrderClaimUseCase = mock(AdminOrderClaimUseCase.class);
        OrderClaimView view = view();

        when(orderClaimUseCase.requestMemberClaim(eq(200L), eq(CUSTOMER_USER_ID), any()))
                .thenReturn(view);
        when(orderClaimUseCase.listGuestClaims(200L, "guest-token"))
                .thenReturn(List.of(view));
        when(adminOrderClaimUseCase.list(OrderClaimStatus.REQUESTED, "cursor-current", 50))
                .thenReturn(new CursorPage<>(List.of(view), "cursor-next", true));
        when(adminOrderClaimUseCase.resolve(eq(10L), eq(ADMIN_USER_ID), any())).thenReturn(view);
        when(adminOrderClaimUseCase.completeExchange(eq(10L), eq(ADMIN_USER_ID), any()))
                .thenReturn(view);

        mockMvc = mockMvc(
                restDocumentation,
                new MeOrderClaimController(orderClaimUseCase),
                new GuestOrderClaimController(orderClaimUseCase),
                new AdminOrderClaimController(adminOrderClaimUseCase));
    }

    @Test
    @DisplayName("회원 주문 클레임 접수 API를 문서화한다")
    void request_member_order_claim() throws Exception {
        mockMvc.perform(post("/api/v1/me/orders/200/claims")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content(requestBody()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("주문 클레임 접수 API는 품목 100건을 초과한 요청을 거절한다")
    void request_member_order_claim_rejects_more_than_100_items() throws Exception {
        String items = "[" + String.join(",", Collections.nCopies(
                101, "{\"orderItemId\":300,\"quantity\":1}")) + "]";

        mockMvc.perform(post("/api/v1/me/orders/200/claims")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "DAMAGED",
                                  "requestedResolution": "REFUND",
                                  "reason": "수령한 상품이 파손되었습니다.",
                                  "items": %s
                                }
                                """.formatted(items)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("주문 클레임 접수 API는 null 품목을 검증 오류로 거절한다")
    void request_member_order_claim_rejects_null_item() throws Exception {
        mockMvc.perform(post("/api/v1/me/orders/200/claims")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "DAMAGED",
                                  "requestedResolution": "REFUND",
                                  "reason": "수령한 상품이 파손되었습니다.",
                                  "items": [null]
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비회원 주문 클레임 조회 API를 문서화한다")
    void list_guest_order_claims() throws Exception {
        mockMvc.perform(get("/api/v1/orders/200/claims")
                        .header("X-Access-Token", "guest-token"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 클레임 목록 API를 문서화한다")
    void list_admin_order_claims() throws Exception {
        mockMvc.perform(get("/api/v1/admin/order-claims")
                        .with(adminUser())
                        .param("status", "REQUESTED")
                        .param("cursor", "cursor-current")
                        .param("size", "50"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 클레임 처리 API를 문서화한다")
    void resolve_order_claim() throws Exception {
        mockMvc.perform(post("/api/v1/admin/order-claims/10/resolve")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true,
                                  "refundAmount": 43000,
                                  "restoreInventory": true,
                                  "note": "반품 확인 후 환불"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 클레임 처리는 재고 복구 여부를 명시해야 한다")
    void resolve_order_claim_requires_inventory_decision() throws Exception {
        mockMvc.perform(post("/api/v1/admin/order-claims/10/resolve")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "approved": true,
                                  "refundAmount": 43000,
                                  "note": "재고 판단이 누락됨"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("관리자 교환 완료 API를 문서화한다")
    void complete_order_exchange() throws Exception {
        mockMvc.perform(post("/api/v1/admin/order-claims/10/complete-exchange")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "carrier": "테스트택배",
                                  "trackingNumber": "TRACK-2",
                                  "note": "교환품 발송"
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 교환 완료 API는 배송 정보를 모두 요구한다")
    void complete_order_exchange_requires_delivery_information() throws Exception {
        mockMvc.perform(post("/api/v1/admin/order-claims/10/complete-exchange")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "carrier": " ",
                                  "trackingNumber": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    private static String requestBody() {
        return """
                {
                  "type": "DAMAGED",
                  "requestedResolution": "REFUND",
                  "reason": "수령한 상품이 파손되었습니다.",
                  "items": [
                    {
                      "orderItemId": 300,
                      "quantity": 1
                    }
                  ]
                }
                """;
    }

    private static OrderClaimView view() {
        return new OrderClaimView(
                10L,
                200L,
                OrderClaimType.DAMAGED,
                OrderClaimResolution.REFUND,
                OrderClaimStatus.REQUESTED,
                "수령한 상품이 파손되었습니다.",
                null,
                null,
                null,
                null,
                null,
                43_000L,
                null,
                null,
                LocalDateTime.of(2026, 7, 24, 10, 0),
                null,
                null,
                List.of(new OrderClaimView.Item(
                        300L, 400L, "빈티지 가죽 소품", 1, 40_000L)));
    }
}
