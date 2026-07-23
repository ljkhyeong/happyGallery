package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminDashboardController;
import com.personal.happygallery.adapter.in.web.admin.AdminInquiryController;
import com.personal.happygallery.adapter.in.web.admin.AdminNoticeController;
import com.personal.happygallery.adapter.in.web.admin.AdminProductQnaController;
import com.personal.happygallery.application.dashboard.dto.DailyRevenue;
import com.personal.happygallery.application.dashboard.dto.DashboardOverview;
import com.personal.happygallery.application.dashboard.dto.Granularity;
import com.personal.happygallery.application.dashboard.dto.PeriodSalesSummary;
import com.personal.happygallery.application.dashboard.dto.RefundStats;
import com.personal.happygallery.application.dashboard.dto.RevenueBreakdown;
import com.personal.happygallery.application.dashboard.dto.SlotUtilization;
import com.personal.happygallery.application.dashboard.dto.StatusCount;
import com.personal.happygallery.application.dashboard.dto.TopProduct;
import com.personal.happygallery.application.dashboard.port.in.DashboardQueryUseCase;
import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeAdminUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.notice.Notice;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminDashboardContentApiRestDocsTest extends RestDocsTestSupport {

    private static final String SNIPPET_GROUP = "admin-api-rest-docs-test";

    private MockMvc mockMvc;
    private DashboardQueryUseCase dashboardQueryUseCase;
    private NoticeAdminUseCase noticeAdminUseCase;
    private NoticeQueryUseCase noticeQueryUseCase;
    private ProductQnaUseCase qnaUseCase;
    private InquiryUseCase inquiryUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        dashboardQueryUseCase = mock(DashboardQueryUseCase.class);
        noticeAdminUseCase = mock(NoticeAdminUseCase.class);
        noticeQueryUseCase = mock(NoticeQueryUseCase.class);
        qnaUseCase = mock(ProductQnaUseCase.class);
        inquiryUseCase = mock(InquiryUseCase.class);

        Notice notice = RestDocsFixtures.notice();
        ProductQnaUseCase.QnaWithAuthor qna =
                new ProductQnaUseCase.QnaWithAuthor(RestDocsFixtures.productQna(), "홍길동");
        InquiryUseCase.InquiryWithUser inquiry =
                new InquiryUseCase.InquiryWithUser(RestDocsFixtures.inquiry(), "홍길동");

        stubDashboard();
        when(noticeQueryUseCase.listAll()).thenReturn(List.of(notice));
        when(noticeQueryUseCase.getDetail(1L)).thenReturn(notice);
        when(noticeAdminUseCase.create(any(), any(), anyBoolean())).thenReturn(notice);
        when(noticeAdminUseCase.update(eq(1L), any(), any(), anyBoolean())).thenReturn(notice);
        when(qnaUseCase.listByProduct(1L)).thenReturn(List.of(qna));
        when(qnaUseCase.replyAndGet(eq(5L), any(), eq(ADMIN_USER_ID))).thenReturn(qna);
        when(inquiryUseCase.listAll(isNull(), anyInt()))
                .thenReturn(new CursorPage<>(List.of(inquiry), null, false));
        when(inquiryUseCase.findByIdForAdmin(9L)).thenReturn(inquiry);
        when(inquiryUseCase.replyAndGet(eq(9L), any(), eq(ADMIN_USER_ID))).thenReturn(inquiry);

        mockMvc = mockMvc(restDocumentation, SNIPPET_GROUP,
                new AdminDashboardController(dashboardQueryUseCase),
                new AdminNoticeController(noticeAdminUseCase, noticeQueryUseCase),
                new AdminProductQnaController(qnaUseCase),
                new AdminInquiryController(inquiryUseCase));
    }

    @Test
    @DisplayName("관리자 대시보드 요약 API를 문서화한다")
    void admin_dashboard_overview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/overview")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 매출 요약 API를 문서화한다")
    void admin_dashboard_sales_summary() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/sales-summary")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31")
                        .param("granularity", "DAILY"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 매출 분해 API를 문서화한다")
    void admin_dashboard_revenue_breakdown() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/revenue-breakdown")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 주문 상태 분포 API를 문서화한다")
    void admin_dashboard_order_status() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/order-status").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 환불 통계 API를 문서화한다")
    void admin_dashboard_refunds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/refunds")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 인기 상품 API를 문서화한다")
    void admin_dashboard_top_products() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/top-products")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31")
                        .param("limit", "10")
                        .param("sort", "REVENUE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 일별 매출 API를 문서화한다")
    void admin_dashboard_daily_revenue() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/daily-revenue")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 슬롯 이용률 API를 문서화한다")
    void admin_dashboard_slot_utilization() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/slot-utilization")
                        .with(adminUser())
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 공지 목록 API를 문서화한다")
    void admin_list_notices() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notices").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 공지 생성 API를 문서화한다")
    void admin_create_notice() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notices")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "운영 안내",
                                  "content": "5월 클래스 운영 안내입니다.",
                                  "pinned": true
                                }
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 공지 수정 API를 문서화한다")
    void admin_update_notice() throws Exception {
        mockMvc.perform(put("/api/v1/admin/notices/{id}", 1L)
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "운영 안내",
                                  "content": "수정된 안내입니다.",
                                  "pinned": false
                                }
                                """))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 공지 삭제 API를 문서화한다")
    void admin_delete_notice() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/notices/{id}", 1L).with(adminUser()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 QNA 목록 API를 문서화한다")
    void admin_list_qna() throws Exception {
        mockMvc.perform(get("/api/v1/admin/qna")
                        .with(adminUser())
                        .param("productId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 QNA 답변 API를 문서화한다")
    void admin_reply_qna() throws Exception {
        mockMvc.perform(post("/api/v1/admin/qna/{id}/reply", 5L)
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("{\"replyContent\":\"주문 승인 후 안내드립니다.\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 문의 목록 API를 문서화한다")
    void admin_list_inquiries() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inquiries").with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(9))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    @DisplayName("관리자 문의 상세 API를 문서화한다")
    void admin_get_inquiry() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inquiries/{id}", 9L).with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 문의 답변 API를 문서화한다")
    void admin_reply_inquiry() throws Exception {
        mockMvc.perform(post("/api/v1/admin/inquiries/{id}/reply", 9L)
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("{\"replyContent\":\"마이페이지에서 변경할 수 있습니다.\"}"))
                .andExpect(status().isOk());
    }

    private void stubDashboard() {
        when(dashboardQueryUseCase.getOverview(any(), any())).thenReturn(
                new DashboardOverview(39000L, 1, 1, 2, 120000L, 3));
        when(dashboardQueryUseCase.getSalesSummary(any(), any(), eq(Granularity.DAILY))).thenReturn(
                List.of(new PeriodSalesSummary("2026-05-01", 39000L, 1, 39000L)));
        when(dashboardQueryUseCase.getRevenueBreakdown(any(), any())).thenReturn(
                new RevenueBreakdown(39000L, 5000L, 45000L, 240000L, 329000L));
        when(dashboardQueryUseCase.getOrderStatusDistribution()).thenReturn(
                List.of(new StatusCount("PAID_APPROVAL_PENDING", 1)));
        when(dashboardQueryUseCase.getRefundStats(any(), any())).thenReturn(
                new RefundStats(1, 5000L, 0.1));
        when(dashboardQueryUseCase.getTopProducts(any(), any(), eq(10), any())).thenReturn(
                List.of(new TopProduct(1L, "시그니처 캔들", "READY_STOCK", 39000L, 1)));
        when(dashboardQueryUseCase.getDailyRevenueSeries(any(), any())).thenReturn(
                List.of(new DailyRevenue(LocalDate.of(2026, 5, 1), 39000L)));
        when(dashboardQueryUseCase.getSlotUtilization(any(), any())).thenReturn(
                List.of(new SlotUtilization(LocalDate.of(2026, 5, 7), "향수 원데이", 8, 2, 0.25)));
    }
}
