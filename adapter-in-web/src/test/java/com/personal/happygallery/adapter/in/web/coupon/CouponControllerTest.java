package com.personal.happygallery.adapter.in.web.coupon;

import com.personal.happygallery.adapter.in.web.admin.AdminCouponController;
import com.personal.happygallery.adapter.in.web.admin.dto.CreateCouponRequest;
import com.personal.happygallery.adapter.in.web.customer.MeCouponController;
import com.personal.happygallery.adapter.in.web.customer.dto.ClaimCouponRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.coupon.port.in.CouponAdminUseCase;
import com.personal.happygallery.application.coupon.port.in.CouponDefinitionCommand;
import com.personal.happygallery.application.coupon.port.in.CouponMemberUseCase;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CouponControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 8, 12, 0);

    @DisplayName("관리자 쿠폰 등록 요청을 애플리케이션 명령으로 변환하고 응답한다")
    @Test
    void adminCreate_mapsRequestToCommand() {
        CouponAdminUseCase useCase = Mockito.mock(CouponAdminUseCase.class);
        CouponDefinition definition = definition();
        when(useCase.create(Mockito.any())).thenReturn(definition);
        AdminCouponController controller = new AdminCouponController(useCase);
        CreateCouponRequest request = new CreateCouponRequest(
                "첫 구매 할인",
                CouponDiscountType.FIXED,
                5_000L,
                30_000L,
                null,
                NOW.minusDays(1),
                NOW.plusDays(30),
                true,
                true);

        var response = controller.create(request);

        ArgumentCaptor<CouponDefinitionCommand> commandCaptor =
                ArgumentCaptor.forClass(CouponDefinitionCommand.class);
        verify(useCase).create(commandCaptor.capture());
        assertSoftly(softly -> {
            softly.assertThat(commandCaptor.getValue().discountType())
                    .isEqualTo(CouponDiscountType.FIXED);
            softly.assertThat(commandCaptor.getValue().discountValue()).isEqualTo(5_000L);
            softly.assertThat(response.name()).isEqualTo("첫 구매 할인");
            softly.assertThat(response.publiclyClaimable()).isTrue();
        });
    }

    @DisplayName("회원 쿠폰 발급과 목록 조회는 인증 회원 식별자만 유스케이스에 전달한다")
    @Test
    void memberEndpoints_useAuthenticatedCustomerId() {
        CouponMemberUseCase useCase = Mockito.mock(CouponMemberUseCase.class);
        CouponDefinition definition = definition();
        IssuedCoupon issued = new IssuedCoupon(10L, 20L, NOW);
        var view = new CouponMemberUseCase.IssuedCouponView(issued, definition);
        when(useCase.claim(20L, 10L)).thenReturn(view);
        when(useCase.listMyCoupons(20L)).thenReturn(List.of(view));
        MeCouponController controller = new MeCouponController(useCase);
        CustomerPrincipal customer = new CustomerPrincipal(
                20L,
                "coupon@example.com",
                "쿠폰 회원",
                "01012345678",
                true,
                true,
                0L);

        var claimed = controller.claim(customer, new ClaimCouponRequest(10L));
        var listed = controller.list(customer);

        verify(useCase).claim(20L, 10L);
        verify(useCase).listMyCoupons(20L);
        assertSoftly(softly -> {
            softly.assertThat(claimed.name()).isEqualTo("첫 구매 할인");
            softly.assertThat(claimed.status().name()).isEqualTo("AVAILABLE");
            softly.assertThat(listed).hasSize(1);
            softly.assertThat(listed.getFirst().claimedAt()).isEqualTo(NOW);
        });
    }

    private static CouponDefinition definition() {
        return new CouponDefinition(
                "첫 구매 할인",
                CouponDiscountType.FIXED,
                5_000L,
                30_000L,
                null,
                NOW.minusDays(1),
                NOW.plusDays(30),
                true,
                true);
    }
}
