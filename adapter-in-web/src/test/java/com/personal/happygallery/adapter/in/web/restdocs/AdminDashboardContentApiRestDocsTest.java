package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminDashboardController;
import com.personal.happygallery.adapter.in.web.admin.AdminInquiryController;
import com.personal.happygallery.adapter.in.web.admin.AdminGroupInquiryController;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import com.personal.happygallery.adapter.in.web.admin.AdminNoticeController;
import com.personal.happygallery.adapter.in.web.admin.AdminProductQnaController;
import com.personal.happygallery.adapter.in.web.admin.AdminReviewController;
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
import com.personal.happygallery.application.review.port.in.ReviewUseCase;
import com.personal.happygallery.application.review.port.in.AdminReviewUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.notice.Notice;
import com.personal.happygallery.domain.review.ReviewStatus;
import com.personal.happygallery.domain.review.ReviewTargetType;
import com.personal.happygallery.domain.review.ReviewEvidenceProvenance;
import com.personal.happygallery.domain.review.ReviewModerationActionType;
import com.personal.happygallery.domain.review.ReviewReportReason;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
    private AdminReviewUseCase adminReviewUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        dashboardQueryUseCase = mock(DashboardQueryUseCase.class);
        noticeAdminUseCase = mock(NoticeAdminUseCase.class);
        noticeQueryUseCase = mock(NoticeQueryUseCase.class);
        qnaUseCase = mock(ProductQnaUseCase.class);
        inquiryUseCase = mock(InquiryUseCase.class);
        adminReviewUseCase = mock(AdminReviewUseCase.class);

        Notice notice = RestDocsFixtures.notice();
        ProductQnaUseCase.QnaWithAuthor qna =
                new ProductQnaUseCase.QnaWithAuthor(RestDocsFixtures.productQna(), "홍길동");
        InquiryUseCase.InquiryWithUser inquiry =
                new InquiryUseCase.InquiryWithUser(RestDocsFixtures.inquiry(), "홍길동");
        ReviewUseCase.ReviewItem productReview = RestDocsFixtures.productReviewItem();
        ReviewUseCase.ReviewItem hiddenProductReview = RestDocsFixtures.hiddenProductReviewItem();
        ReviewUseCase.ReviewItem productReviewWithoutReply = new ReviewUseCase.ReviewItem(
                productReview.id(),
                productReview.userId(),
                productReview.authorName(),
                productReview.targetType(),
                productReview.sourceId(),
                productReview.targetId(),
                productReview.targetName(),
                productReview.rating(),
                productReview.content(),
                productReview.status(),
                productReview.contentRevision(),
                productReview.version() + 1L,
                productReview.hiddenReason(),
                productReview.hiddenAt(),
                productReview.hiddenByAdminId(),
                productReview.createdAt(),
                productReview.updatedAt(),
                productReview.editedAt(),
                productReview.edited(),
                productReview.verifiedTransaction(),
                null,
                productReview.helpfulCount(),
                productReview.images());
        ReviewUseCase.ReviewEvidenceItem evidence = new ReviewUseCase.ReviewEvidenceItem(
                81L,
                productReview.contentRevision(),
                productReview.rating(),
                productReview.content(),
                productReview.editedAt(),
                ReviewEvidenceProvenance.LIVE,
                true,
                productReview.images().stream()
                        .map(ReviewUseCase.ReviewImageItem::imageUrl)
                        .toList(),
                LocalDateTime.of(2026, 5, 1, 20, 0));
        ReviewUseCase.ReviewReportItem pendingReport = new ReviewUseCase.ReviewReportItem(
                71L,
                31L,
                CUSTOMER_USER_ID,
                ReviewReportReason.PRIVACY,
                "개인 연락처가 노출되어 있습니다.",
                ReviewStatus.PUBLISHED,
                evidence,
                ReviewReportStatus.PENDING,
                null,
                null,
                null,
                LocalDateTime.of(2026, 5, 1, 20, 0));
        ReviewUseCase.ReviewReportItem acceptedReport = new ReviewUseCase.ReviewReportItem(
                pendingReport.id(),
                pendingReport.reviewId(),
                pendingReport.reporterUserId(),
                pendingReport.reason(),
                pendingReport.detail(),
                pendingReport.snapshotStatus(),
                pendingReport.evidence(),
                ReviewReportStatus.ACCEPTED,
                "개인정보 노출을 확인하여 숨김 처리했습니다.",
                ADMIN_USER_ID,
                LocalDateTime.of(2026, 5, 1, 21, 0),
                pendingReport.createdAt());
        ReviewUseCase.ReviewReportSummaryItem pendingReportSummary =
                new ReviewUseCase.ReviewReportSummaryItem(
                        pendingReport.id(),
                        pendingReport.reviewId(),
                        pendingReport.reason(),
                        pendingReport.snapshotStatus(),
                        pendingReport.status(),
                        pendingReport.createdAt());

        stubDashboard();
        when(noticeQueryUseCase.listAll()).thenReturn(List.of(notice));
        when(noticeQueryUseCase.getDetail(1L)).thenReturn(notice);
        when(noticeAdminUseCase.getForEdit(1L)).thenReturn(notice);
        when(noticeAdminUseCase.create(any(), any(), anyBoolean())).thenReturn(notice);
        when(noticeAdminUseCase.update(eq(1L), anyLong(), any(), any(), anyBoolean())).thenReturn(notice);
        when(qnaUseCase.listByProductForAdmin(1L)).thenReturn(List.of(qna));
        when(qnaUseCase.listByProductForAdmin(eq(1L), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(qna), "cursor-next", true));
        when(qnaUseCase.listUnanswered(isNull(), anyInt()))
                .thenReturn(new CursorPage<>(List.of(qna), null, false));
        when(qnaUseCase.replyAndGet(eq(5L), any(), eq(ADMIN_USER_ID))).thenReturn(qna);
        when(inquiryUseCase.listAll(isNull(), anyInt()))
                .thenReturn(new CursorPage<>(List.of(inquiry), null, false));
        when(inquiryUseCase.findByIdForAdmin(9L)).thenReturn(inquiry);
        when(inquiryUseCase.replyAndGet(eq(9L), any(), eq(ADMIN_USER_ID))).thenReturn(inquiry);
        when(adminReviewUseCase.listAdminReviews(
                eq(ReviewTargetType.PRODUCT),
                eq(ReviewStatus.PUBLISHED),
                isNull(),
                eq(20)))
                .thenReturn(new CursorPage<>(List.of(productReview), "cursor-next", true));
        when(adminReviewUseCase.getAdminReview(31L)).thenReturn(productReview);
        when(adminReviewUseCase.updateStatus(
                eq(31L),
                eq(ReviewStatus.HIDDEN),
                any(),
                eq(productReview.contentRevision()),
                eq(productReview.version()),
                eq(ADMIN_USER_ID)))
                .thenReturn(hiddenProductReview);
        when(adminReviewUseCase.listModerationActions(31L))
                .thenReturn(List.of(new ReviewUseCase.ModerationActionItem(
                        61L,
                        31L,
                        ReviewModerationActionType.HIDE,
                        ReviewStatus.PUBLISHED,
                        ReviewStatus.HIDDEN,
                        "운영 정책 위반 내용",
                        ADMIN_USER_ID,
                        evidence,
                        LocalDateTime.of(2026, 5, 1, 21, 0))));
        when(adminReviewUseCase.upsertOfficialReply(
                eq(31L), any(), eq(productReview.version()), eq(ADMIN_USER_ID)))
                .thenReturn(productReview);
        when(adminReviewUseCase.deleteOfficialReply(
                31L, productReview.version()))
                .thenReturn(productReviewWithoutReply);
        when(adminReviewUseCase.listAdminReports(
                eq(ReviewReportStatus.PENDING), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(
                        List.of(pendingReportSummary), "report-cursor-next", true));
        when(adminReviewUseCase.getAdminReport(71L)).thenReturn(pendingReport);
        when(adminReviewUseCase.decideReport(
                eq(71L), eq(ReviewReportStatus.ACCEPTED), any(), eq(ADMIN_USER_ID)))
                .thenReturn(acceptedReport);

        var groupInquiries = mock(GroupInquiryUseCase.class);
        var groupDetail = GroupInquiryRestDocsFixtures.detail();
        when(groupInquiries.detailForAdmin(51L)).thenReturn(groupDetail);
        when(groupInquiries.listForAdmin(isNull(), isNull(), eq(20)))
                .thenReturn(new CursorPage<>(List.of(groupDetail.view()), null, false));
        when(groupInquiries.createExternal(eq(ADMIN_USER_ID), any())).thenReturn(groupDetail);
        when(groupInquiries.update(51L, 0L, GroupInquiryStatus.CONSULTING, "일정 협의 중", ADMIN_USER_ID)).thenReturn(groupDetail);

        mockMvc = mockMvc(restDocumentation, SNIPPET_GROUP,
                new AdminGroupInquiryController(groupInquiries),
                new AdminDashboardController(dashboardQueryUseCase),
                new AdminNoticeController(noticeAdminUseCase, noticeQueryUseCase),
                new AdminProductQnaController(qnaUseCase),
                new AdminInquiryController(inquiryUseCase),
                new AdminReviewController(adminReviewUseCase));
    }

    @Test
    @DisplayName("관리자 단체 문의 목록을 문서화한다")
    void admin_group_inquiry_list() throws Exception {
        mockMvc.perform(get("/api/v1/admin/group-inquiries").with(adminUser()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(51));
    }
    @Test
    @DisplayName("관리자에게 단체 문의의 연락처와 상담 이력을 제공한다")
    void admin_group_inquiry_detail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/group-inquiries/51").with(adminUser()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.details.phone").value("01012345678"))
                .andExpect(jsonPath("$.activities[0].note").value("일정 협의 중"));
    }
    @Test
    @DisplayName("관리자는 외부 채널로 받은 단체 문의를 등록한다")
    void admin_group_inquiry_create() throws Exception {
        mockMvc.perform(post("/api/v1/admin/group-inquiries").with(adminUser())
                        .contentType(APPLICATION_JSON).content(GroupInquiryRestDocsFixtures.REQUEST))
                .andExpect(status().isCreated());
    }
    @Test
    @DisplayName("관리자는 읽은 버전과 함께 상담 상태와 메모를 저장한다")
    void admin_group_inquiry_update() throws Exception {
        mockMvc.perform(put("/api/v1/admin/group-inquiries/51").with(adminUser())
                        .contentType(APPLICATION_JSON).content("""
                        {"version":0,"status":"CONSULTING","note":"일정 협의 중"}
                        """))
                .andExpect(status().isOk());
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
    @DisplayName("관리자 공지 상세 API를 문서화한다")
    void admin_get_notice() throws Exception {
        mockMvc.perform(get("/api/v1/admin/notices/{id}", 1L).with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(RestDocsFixtures.notice().getContent()))
                .andExpect(jsonPath("$.version").value(RestDocsFixtures.notice().getVersion()));
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
                                  "expectedVersion": 0,
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
        mockMvc.perform(delete("/api/v1/admin/notices/{id}", 1L)
                        .with(adminUser())
                        .param("expectedVersion", "0"))
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
    @DisplayName("관리자 상품별 QNA 커서 페이지 API를 문서화한다")
    void admin_list_qna_page() throws Exception {
        mockMvc.perform(get("/api/v1/admin/qna/page")
                        .with(adminUser())
                        .param("productId", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.nextCursor").value("cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    @DisplayName("관리자 미답변 QNA 작업함 API를 문서화한다")
    void admin_list_unanswered_qna() throws Exception {
        mockMvc.perform(get("/api/v1/admin/qna/unanswered")
                        .with(adminUser())
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(5))
                .andExpect(jsonPath("$.hasMore").value(false));
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

    @Test
    @DisplayName("관리자 후기 필터 커서 페이지 API를 문서화한다")
    void admin_list_reviews() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews")
                        .with(adminUser())
                        .param("targetType", "PRODUCT")
                        .param("status", "PUBLISHED")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(31))
                .andExpect(jsonPath("$.content[0].userId").value(CUSTOMER_USER_ID))
                .andExpect(jsonPath("$.content[0].authorName").value("홍길동"))
                .andExpect(jsonPath("$.content[0].verifiedTransaction").value(true))
                .andExpect(jsonPath("$.content[0].officialReply.adminUserId")
                        .value(ADMIN_USER_ID))
                .andExpect(jsonPath("$.nextCursor").value("cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    @DisplayName("관리자 후기 단건 조회 API를 문서화한다")
    void admin_get_review() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews/{reviewId}", 31L)
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.contentRevision").value(1))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    @DisplayName("관리자 후기 숨김 상태 변경 API를 문서화한다")
    void admin_update_review_status() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/reviews/{reviewId}/status", 31L)
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "HIDDEN",
                                  "reason": "운영 정책 위반 내용",
                                  "expectedContentRevision": 1,
                                  "expectedVersion": 0
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HIDDEN"))
                .andExpect(jsonPath("$.hiddenReason").value("운영 정책 위반 내용"))
                .andExpect(jsonPath("$.hiddenByAdminId").value(ADMIN_USER_ID));
    }

    @Test
    @DisplayName("관리자 후기 공개 상태 감사 이력 API를 문서화한다")
    void admin_list_review_moderation_actions() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reviews/{reviewId}/moderation-actions", 31L)
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(61))
                .andExpect(jsonPath("$[0].action").value("HIDE"))
                .andExpect(jsonPath("$[0].previousStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$[0].newStatus").value("HIDDEN"))
                .andExpect(jsonPath("$[0].adminUserId").value(ADMIN_USER_ID))
                .andExpect(jsonPath("$[0].evidence.contentRevision").value(1))
                .andExpect(jsonPath("$[0].evidence.imagesComplete").value(true));
    }

    @Test
    @DisplayName("관리자가 후기 공식 답글을 작성하거나 수정하는 API를 문서화한다")
    void admin_upsert_review_reply() throws Exception {
        mockMvc.perform(put("/api/v1/admin/reviews/{reviewId}/reply", 31L)
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 0,
                                  "content": "소중한 후기 감사합니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.officialReply.content")
                        .value("소중한 후기 감사합니다."))
                .andExpect(jsonPath("$.officialReply.adminUserId").value(ADMIN_USER_ID));
    }

    @Test
    @DisplayName("관리자가 후기 공식 답글을 삭제하는 API를 문서화한다")
    void admin_delete_review_reply() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/reviews/{reviewId}/reply", 31L)
                        .with(adminUser())
                        .param("expectedVersion", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(31))
                .andExpect(jsonPath("$.officialReply").doesNotExist());
    }

    @Test
    @DisplayName("관리자 후기 신고 커서 페이지 API를 문서화한다")
    void admin_list_review_reports() throws Exception {
        mockMvc.perform(get("/api/v1/admin/review-reports")
                        .with(adminUser())
                        .param("status", "PENDING")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(71))
                .andExpect(jsonPath("$.content[0].reason").value("PRIVACY"))
                .andExpect(jsonPath("$.content[0].snapshotStatus").value("PUBLISHED"))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].reporterUserId").doesNotExist())
                .andExpect(jsonPath("$.content[0].detail").doesNotExist())
                .andExpect(jsonPath("$.content[0].evidence").doesNotExist())
                .andExpect(jsonPath("$.nextCursor").value("report-cursor-next"))
                .andExpect(jsonPath("$.hasMore").value(true));
    }

    @Test
    @DisplayName("계정 기반 관리자가 후기 신고 상세와 증거를 조회하는 API를 문서화한다")
    void admin_get_review_report() throws Exception {
        mockMvc.perform(get("/api/v1/admin/review-reports/{reportId}", 71L)
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(71))
                .andExpect(jsonPath("$.reporterUserId").value(CUSTOMER_USER_ID))
                .andExpect(jsonPath("$.detail").value("개인 연락처가 노출되어 있습니다."))
                .andExpect(jsonPath("$.evidence.rating").value(5))
                .andExpect(jsonPath("$.evidence.content")
                        .value("마감이 깔끔하고 선물하기 좋았습니다."));
    }

    @Test
    @DisplayName("관리자가 후기 신고를 승인하거나 기각하는 API를 문서화한다")
    void admin_decide_review_report() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/review-reports/{reportId}", 71L)
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "decision": "ACCEPTED",
                                  "note": "개인정보 노출을 확인하여 숨김 처리했습니다."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(71))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.decidedByAdminId").value(ADMIN_USER_ID))
                .andExpect(jsonPath("$.decidedAt").exists());
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
