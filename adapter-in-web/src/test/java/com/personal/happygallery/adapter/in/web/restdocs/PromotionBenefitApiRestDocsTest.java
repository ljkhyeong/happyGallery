package com.personal.happygallery.adapter.in.web.restdocs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personal.happygallery.adapter.in.web.admin.AdminCouponController;
import com.personal.happygallery.adapter.in.web.admin.AdminEventController;
import com.personal.happygallery.adapter.in.web.customer.MeCouponController;
import com.personal.happygallery.adapter.in.web.customer.MeRewardController;
import com.personal.happygallery.adapter.in.web.event.EventController;
import com.personal.happygallery.application.coupon.port.in.CouponAdminUseCase;
import com.personal.happygallery.application.coupon.port.in.CouponMemberUseCase;
import com.personal.happygallery.application.event.port.in.EventAdminUseCase;
import com.personal.happygallery.application.event.port.in.EventQueryUseCase;
import com.personal.happygallery.application.reward.port.in.RewardQueryUseCase;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.coupon.IssuedCouponStatus;
import com.personal.happygallery.domain.event.Event;
import com.personal.happygallery.domain.reward.RewardLedgerType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

class PromotionBenefitApiRestDocsTest extends RestDocsTestSupport {

    private static final LocalDateTime START_AT = LocalDateTime.of(2026, 8, 1, 0, 0);
    private static final LocalDateTime END_AT = LocalDateTime.of(2026, 8, 31, 23, 59);

    private MockMvc mockMvc;
    private EventAdminUseCase eventAdminUseCase;
    private CouponAdminUseCase couponAdminUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        EventQueryUseCase eventQueryUseCase = mock(EventQueryUseCase.class);
        eventAdminUseCase = mock(EventAdminUseCase.class);
        CouponMemberUseCase couponMemberUseCase = mock(CouponMemberUseCase.class);
        couponAdminUseCase = mock(CouponAdminUseCase.class);
        RewardQueryUseCase rewardQueryUseCase = mock(RewardQueryUseCase.class);

        Event event = event();
        CouponDefinition definition = couponDefinition();
        IssuedCoupon issuedCoupon = issuedCoupon();
        CouponMemberUseCase.IssuedCouponView issuedView =
                new CouponMemberUseCase.IssuedCouponView(issuedCoupon, definition);

        when(eventQueryUseCase.listPublicEvents()).thenReturn(List.of(event));
        when(eventQueryUseCase.getPublicEvent(1L)).thenReturn(event);
        when(eventAdminUseCase.listAll()).thenReturn(List.of(event));
        when(eventAdminUseCase.getForEdit(1L)).thenReturn(event);
        when(eventAdminUseCase.create(any())).thenReturn(event);
        when(eventAdminUseCase.update(eq(1L), any())).thenReturn(event);

        when(couponMemberUseCase.listClaimableCoupons(CUSTOMER_USER_ID))
                .thenReturn(List.of(definition));
        when(couponMemberUseCase.listMyCoupons(CUSTOMER_USER_ID))
                .thenReturn(List.of(issuedView));
        when(couponMemberUseCase.claim(CUSTOMER_USER_ID, 10L)).thenReturn(issuedView);
        when(couponAdminUseCase.list()).thenReturn(List.of(definition));
        when(couponAdminUseCase.get(10L)).thenReturn(definition);
        when(couponAdminUseCase.create(any())).thenReturn(definition);
        when(couponAdminUseCase.update(eq(10L), anyLong(), any())).thenReturn(definition);

        when(rewardQueryUseCase.getWallet(CUSTOMER_USER_ID)).thenReturn(
                new RewardQueryUseCase.RewardWallet(
                        1_200L,
                        300L,
                        0L,
                        List.of(new RewardQueryUseCase.RewardHistory(
                                30L,
                                RewardLedgerType.EARN,
                                1_500L,
                                1_500L,
                                0L,
                                0L,
                                20L,
                                LocalDateTime.of(2026, 8, 8, 12, 0)))));

        mockMvc = mockMvc(
                restDocumentation,
                new EventController(eventQueryUseCase),
                new AdminEventController(eventAdminUseCase),
                new MeCouponController(couponMemberUseCase),
                new AdminCouponController(couponAdminUseCase),
                new MeRewardController(rewardQueryUseCase));
    }

    @Test
    @DisplayName("공개 이벤트 목록 API를 문서화한다")
    void public_event_list() throws Exception {
        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("공개 이벤트 상세 API를 문서화한다")
    void public_event_detail() throws Exception {
        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 공개 발급 가능 쿠폰 목록 API를 문서화한다")
    void member_claimable_coupon_list() throws Exception {
        mockMvc.perform(get("/api/v1/me/coupons/claimable").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 보유 쿠폰 목록 API를 문서화한다")
    void member_coupon_list() throws Exception {
        mockMvc.perform(get("/api/v1/me/coupons").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원 쿠폰 발급 API를 문서화한다")
    void member_coupon_claim() throws Exception {
        mockMvc.perform(post("/api/v1/me/coupons")
                        .with(customerUser())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"definitionId":10}
                                """))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("회원 적립금 지갑 API를 문서화한다")
    void member_reward_wallet() throws Exception {
        mockMvc.perform(get("/api/v1/me/rewards").with(customerUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 이벤트 목록 API를 문서화한다")
    void admin_event_list() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 이벤트 상세 API를 문서화한다")
    void admin_event_detail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/events/1").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 이벤트 생성 API를 문서화한다")
    void admin_event_create() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content(eventCreateJson()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 이벤트 수정 API를 문서화한다")
    void admin_event_update() throws Exception {
        mockMvc.perform(put("/api/v1/admin/events/1")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content(eventUpdateJson()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 이벤트 삭제 API를 문서화한다")
    void admin_event_delete() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/events/1")
                        .with(adminUser())
                        .param("expectedVersion", "2"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("관리자 쿠폰 목록 API를 문서화한다")
    void admin_coupon_list() throws Exception {
        mockMvc.perform(get("/api/v1/admin/coupons").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 쿠폰 상세 API를 문서화한다")
    void admin_coupon_detail() throws Exception {
        mockMvc.perform(get("/api/v1/admin/coupons/10").with(adminUser()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 쿠폰 생성 API를 문서화한다")
    void admin_coupon_create() throws Exception {
        mockMvc.perform(post("/api/v1/admin/coupons")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content(couponCreateJson()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("관리자 쿠폰 수정 API를 문서화한다")
    void admin_coupon_update() throws Exception {
        mockMvc.perform(put("/api/v1/admin/coupons/10")
                        .with(adminUser())
                        .contentType(APPLICATION_JSON)
                        .content(couponUpdateJson()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 쿠폰 비활성화 API를 문서화한다")
    void admin_coupon_delete() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/coupons/10")
                        .with(adminUser())
                        .param("expectedVersion", "3"))
                .andExpect(status().isNoContent());
    }

    private Event event() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn(1L);
        when(event.getTitle()).thenReturn("여름 공방전");
        when(event.getSummary()).thenReturn("여름 작품과 회원 혜택을 만나는 행사");
        when(event.getContent()).thenReturn("행사 기간과 관련 작품을 확인해 주세요.");
        when(event.getImageUrl()).thenReturn("/api/v1/media/images/11111111-1111-4111-8111-111111111111.jpg");
        when(event.getStartAt()).thenReturn(START_AT);
        when(event.getEndAt()).thenReturn(END_AT);
        when(event.isPublished()).thenReturn(true);
        when(event.isFeatured()).thenReturn(true);
        when(event.getCouponDefinitionId()).thenReturn(10L);
        when(event.getRelatedProductIds()).thenReturn(Set.of(1L, 2L));
        when(event.getVersion()).thenReturn(2L);
        return event;
    }

    private CouponDefinition couponDefinition() {
        CouponDefinition definition = mock(CouponDefinition.class);
        when(definition.getId()).thenReturn(10L);
        when(definition.getName()).thenReturn("여름 10% 할인");
        when(definition.getDiscountType()).thenReturn(CouponDiscountType.PERCENT);
        when(definition.getDiscountValue()).thenReturn(10L);
        when(definition.getMinOrderAmount()).thenReturn(30_000L);
        when(definition.getMaxDiscountAmount()).thenReturn(10_000L);
        when(definition.getValidFrom()).thenReturn(START_AT);
        when(definition.getValidUntil()).thenReturn(END_AT);
        when(definition.isActive()).thenReturn(true);
        when(definition.isPubliclyClaimable()).thenReturn(true);
        when(definition.getVersion()).thenReturn(3L);
        return definition;
    }

    private IssuedCoupon issuedCoupon() {
        IssuedCoupon coupon = mock(IssuedCoupon.class);
        when(coupon.getId()).thenReturn(15L);
        when(coupon.getStatus()).thenReturn(IssuedCouponStatus.AVAILABLE);
        when(coupon.getClaimedAt()).thenReturn(LocalDateTime.of(2026, 8, 8, 11, 0));
        return coupon;
    }

    private String eventCreateJson() {
        return """
                {
                  "title":"여름 공방전",
                  "summary":"여름 작품과 회원 혜택을 만나는 행사",
                  "content":"행사 기간과 관련 작품을 확인해 주세요.",
                  "imageUrl":null,
                  "startAt":"2026-08-01T00:00:00",
                  "endAt":"2026-08-31T23:59:00",
                  "published":true,
                  "featured":true,
                  "couponDefinitionId":10,
                  "relatedProductIds":[1,2]
                }
                """;
    }

    private String eventUpdateJson() {
        return """
                {
                  "expectedVersion":2,
                  "title":"여름 공방전",
                  "summary":"여름 작품과 회원 혜택을 만나는 행사",
                  "content":"행사 기간과 관련 작품을 확인해 주세요.",
                  "imageUrl":null,
                  "startAt":"2026-08-01T00:00:00",
                  "endAt":"2026-08-31T23:59:00",
                  "published":true,
                  "featured":true,
                  "couponDefinitionId":10,
                  "relatedProductIds":[1,2]
                }
                """;
    }

    private String couponCreateJson() {
        return """
                {
                  "name":"여름 10% 할인",
                  "discountType":"PERCENT",
                  "discountValue":10,
                  "minOrderAmount":30000,
                  "maxDiscountAmount":10000,
                  "validFrom":"2026-08-01T00:00:00",
                  "validUntil":"2026-08-31T23:59:00",
                  "active":true,
                  "publiclyClaimable":true
                }
                """;
    }

    private String couponUpdateJson() {
        return """
                {
                  "expectedVersion":3,
                  "name":"여름 10% 할인",
                  "discountType":"PERCENT",
                  "discountValue":10,
                  "minOrderAmount":30000,
                  "maxDiscountAmount":10000,
                  "validFrom":"2026-08-01T00:00:00",
                  "validUntil":"2026-08-31T23:59:00",
                  "active":true,
                  "publiclyClaimable":true
                }
                """;
    }
}
