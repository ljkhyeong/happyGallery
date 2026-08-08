package com.personal.happygallery.application.coupon;

import com.personal.happygallery.adapter.out.persistence.coupon.CouponDefinitionRepository;
import com.personal.happygallery.adapter.out.persistence.coupon.IssuedCouponRepository;
import com.personal.happygallery.adapter.out.persistence.order.OrderRepository;
import com.personal.happygallery.adapter.out.persistence.payment.PaymentAttemptRepository;
import com.personal.happygallery.application.coupon.port.in.CouponAdminUseCase;
import com.personal.happygallery.application.coupon.port.in.CouponDefinitionCommand;
import com.personal.happygallery.application.coupon.port.in.CouponMemberUseCase;
import com.personal.happygallery.application.coupon.port.in.CouponQuote;
import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.CouponDiscountType;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.coupon.IssuedCouponStatus;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.payment.PaymentAttempt;
import com.personal.happygallery.domain.payment.PaymentContext;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class CouponUseCaseIT {

    @Autowired CouponAdminUseCase couponAdminUseCase;
    @Autowired CouponMemberUseCase couponMemberUseCase;
    @Autowired CouponRedemptionUseCase couponRedemptionUseCase;
    @Autowired CouponDefinitionRepository definitionRepository;
    @Autowired IssuedCouponRepository issuedCouponRepository;
    @Autowired PaymentAttemptRepository paymentAttemptRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired UserStorePort userStorePort;
    @Autowired TestCleanupSupport cleanupSupport;
    @Autowired Clock clock;
    @Autowired PlatformTransactionManager transactionManager;

    @AfterEach
    void tearDown() {
        issuedCouponRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        paymentAttemptRepository.deleteAllInBatch();
        definitionRepository.deleteAllInBatch();
        cleanupSupport.clearUsers();
    }

    @DisplayName("관리자는 쿠폰 정의를 생성·수정·조회하고 삭제 시 발급 이력을 위해 비활성화한다")
    @Test
    void adminCrud_preservesDefinitionBySoftDelete() {
        LocalDateTime now = now();
        CouponDefinition created = couponAdminUseCase.create(fixedCommand(now, 10_000L));

        CouponDefinition updated = couponAdminUseCase.update(
                created.getId(),
                created.getVersion(),
                new CouponDefinitionCommand(
                        "여름 20% 할인",
                        CouponDiscountType.PERCENT,
                        20L,
                        50_000L,
                        15_000L,
                        now.minusDays(1),
                        now.plusDays(30),
                        true,
                        true));

        assertSoftly(softly -> {
            softly.assertThat(couponAdminUseCase.list())
                    .extracting(CouponDefinition::getId)
                    .containsExactly(created.getId());
            softly.assertThat(updated.getDiscountType()).isEqualTo(CouponDiscountType.PERCENT);
            softly.assertThat(updated.getDiscountValue()).isEqualTo(20L);
            softly.assertThat(updated.getMaxDiscountAmount()).isEqualTo(15_000L);
            softly.assertThat(updated.getVersion()).isEqualTo(1L);
        });

        couponAdminUseCase.delete(updated.getId(), updated.getVersion());

        CouponDefinition deleted = definitionRepository.findById(updated.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(deleted.isActive()).isFalse();
            softly.assertThat(deleted.isPubliclyClaimable()).isFalse();
            softly.assertThat(deleted.getVersion()).isEqualTo(2L);
        });
    }

    @DisplayName("발급 이력이 생긴 쿠폰은 활성·공개 플래그만 바꿀 수 있고 경제 조건은 소급 변경할 수 없다")
    @Test
    void adminUpdate_issuedDefinitionLocksTermsButAllowsFlags() {
        LocalDateTime now = now();
        User user = createUser("coupon-audit@example.com", "01010001500");
        CouponDefinition definition = couponAdminUseCase.create(fixedCommand(now, 5_000L));
        couponMemberUseCase.claim(user.getId(), definition.getId());

        assertThatThrownBy(() -> couponAdminUseCase.update(
                definition.getId(),
                definition.getVersion(),
                new CouponDefinitionCommand(
                        "소급 변경 이름",
                        definition.getDiscountType(),
                        definition.getDiscountValue(),
                        definition.getMinOrderAmount(),
                        definition.getMaxDiscountAmount(),
                        definition.getValidFrom(),
                        definition.getValidUntil(),
                        true,
                        true)))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertThat(exception.getErrorCode())
                            .isEqualTo(ErrorCode.COUPON_TERMS_IMMUTABLE);
                    assertThat(exception).hasMessageContaining("이미 발급된 쿠폰");
                });

        CouponDefinition deactivated = couponAdminUseCase.update(
                definition.getId(),
                definition.getVersion(),
                new CouponDefinitionCommand(
                        definition.getName(),
                        definition.getDiscountType(),
                        definition.getDiscountValue(),
                        definition.getMinOrderAmount(),
                        definition.getMaxDiscountAmount(),
                        definition.getValidFrom(),
                        definition.getValidUntil(),
                        false,
                        false));

        assertSoftly(softly -> {
            softly.assertThat(deactivated.isActive()).isFalse();
            softly.assertThat(deactivated.isPubliclyClaimable()).isFalse();
            softly.assertThat(deactivated.getVersion()).isEqualTo(1L);
        });
    }

    @DisplayName("회원은 공개 쿠폰을 정의별 한 번만 발급받고 자신의 쿠폰 목록을 조회한다")
    @Test
    void memberClaim_isUniquePerDefinition() {
        User user = createUser("coupon-member@example.com", "01010002000");
        CouponDefinition definition = couponAdminUseCase.create(fixedCommand(now(), 5_000L));

        assertThat(couponMemberUseCase.listClaimableCoupons(user.getId()))
                .extracting(CouponDefinition::getId)
                .containsExactly(definition.getId());

        var claimed = couponMemberUseCase.claim(user.getId(), definition.getId());

        assertSoftly(softly -> {
            softly.assertThat(claimed.issuedCoupon().getStatus())
                    .isEqualTo(IssuedCouponStatus.AVAILABLE);
            softly.assertThat(claimed.issuedCoupon().getUserId()).isEqualTo(user.getId());
            softly.assertThat(couponMemberUseCase.listMyCoupons(user.getId()))
                    .singleElement()
                    .satisfies(view -> {
                        softly.assertThat(view.definition().getName()).isEqualTo("신규 회원 할인");
                        softly.assertThat(view.issuedCoupon().getId())
                                .isEqualTo(claimed.issuedCoupon().getId());
                    });
            softly.assertThat(couponMemberUseCase.listClaimableCoupons(user.getId())).isEmpty();
        });

        assertThatThrownBy(() -> couponMemberUseCase.claim(user.getId(), definition.getId()))
                .isInstanceOfSatisfying(HappyGalleryException.class, exception -> {
                    assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(exception).hasMessage("이미 발급받은 쿠폰입니다.");
                });
    }

    @DisplayName("발급 가능 쿠폰은 이미 발급한 정의를 제외한 최신 100개만 조회한다")
    @Test
    void listClaimableCoupons_excludesClaimedBeforeLimitingToRecentHundred() {
        LocalDateTime now = now();
        User user = createUser("coupon-claimable@example.com", "01010002100");
        List<CouponDefinition> definitions = definitionRepository.saveAllAndFlush(
                IntStream.rangeClosed(1, 102)
                        .mapToObj(index -> new CouponDefinition(
                                "발급 가능 쿠폰 " + index,
                                CouponDiscountType.FIXED,
                                1_000L,
                                0L,
                                null,
                                now.minusDays(1),
                                now.plusDays(30),
                                true,
                                true))
                        .toList());
        CouponDefinition newest = definitions.getLast();
        couponMemberUseCase.claim(user.getId(), newest.getId());
        List<Long> expectedIds = definitions.stream()
                .map(CouponDefinition::getId)
                .filter(id -> !id.equals(newest.getId()))
                .sorted(Comparator.reverseOrder())
                .limit(100)
                .toList();

        List<Long> actualIds = couponMemberUseCase.listClaimableCoupons(user.getId()).stream()
                .map(CouponDefinition::getId)
                .toList();

        assertThat(actualIds).containsExactlyElementsOf(expectedIds);
    }

    @DisplayName("한 발급 트랜잭션이 정의 공유 잠금을 보유해도 다른 회원은 같은 쿠폰을 발급받는다")
    @Test
    void claim_differentMemberProceedsWhileSharedDefinitionLockIsHeld() throws Exception {
        LocalDateTime now = now();
        User user = createUser("coupon-parallel@example.com", "01010002150");
        CouponDefinition definition = couponAdminUseCase.create(fixedCommand(now, 5_000L));
        CountDownLatch sharedLockHeld = new CountDownLatch(1);
        CountDownLatch releaseSharedLock = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var lockHolder = executor.submit(() ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        definitionRepository.findByIdForClaim(definition.getId()).orElseThrow();
                        sharedLockHeld.countDown();
                        await(releaseSharedLock);
                    }));

            assertThat(sharedLockHeld.await(10, TimeUnit.SECONDS)).isTrue();
            var parallelClaim = executor.submit(
                    () -> couponMemberUseCase.claim(user.getId(), definition.getId()));
            try {
                assertThat(parallelClaim.get(5, TimeUnit.SECONDS).issuedCoupon().getUserId())
                        .isEqualTo(user.getId());
            } finally {
                releaseSharedLock.countDown();
            }
            lockHolder.get(5, TimeUnit.SECONDS);
        }
    }

    @DisplayName("같은 회원이 쿠폰을 동시에 발급해도 한 건만 생성되고 중복 요청은 충돌로 수렴한다")
    @Test
    void claim_sameMemberConcurrently_issuesOnlyOnce() throws Exception {
        LocalDateTime now = now();
        User user = createUser("coupon-same-member@example.com", "01010002160");
        CouponDefinition definition = couponAdminUseCase.create(fixedCommand(now, 5_000L));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2)) {
            var attempts = IntStream.range(0, 2)
                    .mapToObj(ignored -> executor.submit(() -> {
                        ready.countDown();
                        await(start);
                        try {
                            couponMemberUseCase.claim(user.getId(), definition.getId());
                            return "ISSUED";
                        } catch (HappyGalleryException exception) {
                            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                            return "CONFLICT";
                        }
                    }))
                    .toList();

            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(List.of(
                    attempts.get(0).get(10, TimeUnit.SECONDS),
                    attempts.get(1).get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder("ISSUED", "CONFLICT");
        }

        assertThat(issuedCouponRepository.count()).isEqualTo(1L);
    }

    @DisplayName("비활성화된 정의의 미사용 쿠폰은 내 목록 조회 시 취소 상태로 정리한다")
    @Test
    void listMyCoupons_cancelsAvailableCouponFromInactiveDefinition() {
        User user = createUser("coupon-inactive@example.com", "01010002200");
        CouponDefinition definition = couponAdminUseCase.create(fixedCommand(now(), 5_000L));
        IssuedCoupon issued = couponMemberUseCase.claim(user.getId(), definition.getId())
                .issuedCoupon();
        couponAdminUseCase.delete(definition.getId(), definition.getVersion());

        var view = couponMemberUseCase.listMyCoupons(user.getId()).getFirst();

        assertSoftly(softly -> {
            softly.assertThat(view.issuedCoupon().getId()).isEqualTo(issued.getId());
            softly.assertThat(view.issuedCoupon().getStatus()).isEqualTo(IssuedCouponStatus.CANCELED);
            softly.assertThat(issuedCouponRepository.findById(issued.getId()).orElseThrow().getStatus())
                    .isEqualTo(IssuedCouponStatus.CANCELED);
        });
    }

    @DisplayName("내 쿠폰 목록은 발급 시각과 식별자 역순의 최근 100개만 조회한다")
    @Test
    void listMyCoupons_limitsStableRecentHistory() {
        LocalDateTime now = now();
        User user = createUser("coupon-history@example.com", "01010002500");
        List<CouponDefinition> definitions = definitionRepository.saveAllAndFlush(
                IntStream.rangeClosed(1, 101)
                        .mapToObj(index -> new CouponDefinition(
                                "이력 쿠폰 " + index,
                                CouponDiscountType.FIXED,
                                1_000L,
                                0L,
                                null,
                                now.minusDays(1),
                                now.plusDays(30),
                                true,
                                false))
                        .toList());
        List<IssuedCoupon> issued = issuedCouponRepository.saveAllAndFlush(
                definitions.stream()
                        .map(definition -> new IssuedCoupon(
                                definition.getId(), user.getId(), now))
                        .toList());
        List<Long> expectedIds = issued.stream()
                .map(IssuedCoupon::getId)
                .sorted(Comparator.reverseOrder())
                .limit(100)
                .toList();

        List<Long> actualIds = couponMemberUseCase.listMyCoupons(user.getId()).stream()
                .map(view -> view.issuedCoupon().getId())
                .toList();

        assertThat(actualIds).containsExactlyElementsOf(expectedIds);
    }

    @DisplayName("주문 쿠폰은 상품 금액 견적 뒤 결제 시도에 예약되고 주문 사용과 전액 취소 복원을 수행한다")
    @Test
    void redemptionFlow_reservesRedeemsAndRestores() {
        LocalDateTime now = now();
        User user = createUser("coupon-order@example.com", "01010003000");
        CouponDefinition definition = couponAdminUseCase.create(fixedCommand(now, 30_000L));
        IssuedCoupon issued = couponMemberUseCase.claim(user.getId(), definition.getId())
                .issuedCoupon();

        CouponQuote none = couponRedemptionUseCase.quoteAndLock(null, null, 100_000L, now);
        CouponQuote quote = couponRedemptionUseCase.quoteAndLock(
                user.getId(), issued.getId(), 100_000L, now);
        PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(
                PaymentAttempt.startForMember(
                        "coupon-payment-attempt",
                        PaymentContext.ORDER,
                        quote.discountedProductAmount(),
                        "{}",
                        user.getId()));

        couponRedemptionUseCase.reserve(issued.getId(), attempt.getId());
        IssuedCoupon reserved = issuedCouponRepository.findById(issued.getId()).orElseThrow();

        Order order = orderRepository.saveAndFlush(Order.forMember(
                user.getId(),
                quote.discountedProductAmount(),
                0L,
                now,
                now.plusHours(24)));
        couponRedemptionUseCase.redeem(issued.getId(), attempt.getId(), order.getId());
        IssuedCoupon redeemed = issuedCouponRepository.findById(issued.getId()).orElseThrow();

        couponRedemptionUseCase.restoreAfterFullCancellation(
                issued.getId(), order.getId(), now.plusHours(1));
        IssuedCoupon restored = issuedCouponRepository.findById(issued.getId()).orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(none.applied()).isFalse();
            softly.assertThat(none.discountAmount()).isZero();
            softly.assertThat(quote.discountAmount()).isEqualTo(30_000L);
            softly.assertThat(quote.discountedProductAmount()).isEqualTo(70_000L);
            softly.assertThat(reserved.getStatus()).isEqualTo(IssuedCouponStatus.RESERVED);
            softly.assertThat(reserved.getPaymentAttemptId()).isEqualTo(attempt.getId());
            softly.assertThat(redeemed.getStatus()).isEqualTo(IssuedCouponStatus.REDEEMED);
            softly.assertThat(redeemed.getUsedOrderId()).isEqualTo(order.getId());
            softly.assertThat(restored.getStatus()).isEqualTo(IssuedCouponStatus.AVAILABLE);
            softly.assertThat(restored.getPaymentAttemptId()).isNull();
            softly.assertThat(restored.getUsedOrderId()).isNull();
        });
    }

    @DisplayName("전액 취소 시 유효기간이 지난 사용 쿠폰은 주문 연결을 보존한 만료 상태가 된다")
    @Test
    void restoreAfterFullCancellation_expiredCouponKeepsAuditLink() {
        LocalDateTime now = now();
        User user = createUser("coupon-expired-refund@example.com", "01010003500");
        CouponDefinition definition = couponAdminUseCase.create(new CouponDefinitionCommand(
                "만료 환불 쿠폰",
                CouponDiscountType.FIXED,
                5_000L,
                0L,
                null,
                now.minusDays(1),
                now.plusHours(1),
                true,
                true));
        IssuedCoupon issued = couponMemberUseCase.claim(user.getId(), definition.getId())
                .issuedCoupon();
        PaymentAttempt attempt = paymentAttemptRepository.saveAndFlush(
                PaymentAttempt.startForMember(
                        "expired-coupon-attempt", PaymentContext.ORDER, 45_000L, "{}", user.getId()));
        couponRedemptionUseCase.reserve(issued.getId(), attempt.getId());
        Order order = orderRepository.saveAndFlush(Order.forMember(
                user.getId(), 45_000L, now, now.plusHours(24)));
        couponRedemptionUseCase.redeem(issued.getId(), attempt.getId(), order.getId());

        couponRedemptionUseCase.restoreAfterFullCancellation(
                issued.getId(), order.getId(), now.plusHours(2));

        IssuedCoupon expired = issuedCouponRepository.findById(issued.getId()).orElseThrow();
        assertSoftly(softly -> {
            softly.assertThat(expired.getStatus()).isEqualTo(IssuedCouponStatus.EXPIRED);
            softly.assertThat(expired.getPaymentAttemptId()).isEqualTo(attempt.getId());
            softly.assertThat(expired.getUsedOrderId()).isEqualTo(order.getId());
            softly.assertThat(expired.getReservedAt()).isNotNull();
            softly.assertThat(expired.getUsedAt()).isNotNull();
        });
    }

    @DisplayName("회원은 다른 회원이 발급받은 쿠폰의 견적을 조회할 수 없다")
    @Test
    void quote_hidesOtherMembersCoupon() {
        LocalDateTime now = now();
        User owner = createUser("coupon-owner@example.com", "01010004000");
        User other = createUser("coupon-other@example.com", "01010005000");
        CouponDefinition definition = couponAdminUseCase.create(fixedCommand(now, 5_000L));
        IssuedCoupon issued = couponMemberUseCase.claim(owner.getId(), definition.getId())
                .issuedCoupon();

        assertThatThrownBy(() -> couponRedemptionUseCase.quoteAndLock(
                other.getId(), issued.getId(), 50_000L, now))
                .isInstanceOfSatisfying(HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND));
    }

    private CouponDefinitionCommand fixedCommand(LocalDateTime now, long discountValue) {
        return new CouponDefinitionCommand(
                "신규 회원 할인",
                CouponDiscountType.FIXED,
                discountValue,
                0L,
                null,
                now.minusDays(1),
                now.plusDays(30),
                true,
                true);
    }

    private User createUser(String email, String phone) {
        return userStorePort.save(new User(email, "password-hash", "쿠폰 회원", phone));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("쿠폰 동시성 테스트 신호를 기다리지 못했습니다.");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("쿠폰 발급 잠금 대기가 중단되었습니다.", exception);
        }
    }
}
