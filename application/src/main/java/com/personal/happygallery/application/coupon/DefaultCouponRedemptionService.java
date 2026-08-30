package com.personal.happygallery.application.coupon;

import com.personal.happygallery.application.coupon.port.in.CouponQuote;
import com.personal.happygallery.application.coupon.port.in.CouponRedemptionUseCase;
import com.personal.happygallery.application.coupon.port.out.CouponDefinitionReaderPort;
import com.personal.happygallery.application.coupon.port.out.IssuedCouponReaderPort;
import com.personal.happygallery.application.coupon.port.out.IssuedCouponStorePort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.payment.PaymentAmountPolicy;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultCouponRedemptionService implements CouponRedemptionUseCase {

    private final CouponDefinitionReaderPort definitionReader;
    private final IssuedCouponReaderPort issuedCouponReader;
    private final IssuedCouponStorePort issuedCouponStore;
    private final Clock clock;

    public DefaultCouponRedemptionService(CouponDefinitionReaderPort definitionReader,
                                          IssuedCouponReaderPort issuedCouponReader,
                                          IssuedCouponStorePort issuedCouponStore,
                                          Clock clock) {
        this.definitionReader = definitionReader;
        this.issuedCouponReader = issuedCouponReader;
        this.issuedCouponStore = issuedCouponStore;
        this.clock = clock;
    }

    @Override
    public CouponQuote quoteAndLock(Long userId,
                                    Long issuedCouponId,
                                    long productAmount,
                                    LocalDateTime now) {
        if (issuedCouponId == null) {
            return CouponQuote.none(productAmount);
        }
        PaymentAmountPolicy.requireValid(productAmount);
        requireUserId(userId);
        IssuedCoupon issuedCoupon = lockIssuedCoupon(issuedCouponId);
        if (!issuedCoupon.isOwnedBy(userId)) {
            throw new NotFoundException("쿠폰");
        }
        CouponDefinition definition = definitionReader
                .findByIdForSharedLock(issuedCoupon.getDefinitionId())
                .orElseThrow(NotFoundException.supplier("쿠폰 정의"));
        issuedCoupon.requireAvailableAt(definition.getValidUntil(), now);
        long discountAmount = definition.calculateDiscount(productAmount, now);
        return new CouponQuote(
                issuedCoupon.getId(),
                definition.getId(),
                definition.getName(),
                productAmount,
                discountAmount);
    }

    @Override
    public void reserve(Long issuedCouponId, Long paymentAttemptId) {
        if (issuedCouponId == null) {
            return;
        }
        IssuedCoupon issuedCoupon = lockIssuedCoupon(issuedCouponId);
        issuedCoupon.reserve(paymentAttemptId, LocalDateTime.now(clock));
        issuedCouponStore.save(issuedCoupon);
    }

    @Override
    public void redeem(Long issuedCouponId, Long paymentAttemptId, Long orderId) {
        if (issuedCouponId == null) {
            return;
        }
        IssuedCoupon issuedCoupon = lockIssuedCoupon(issuedCouponId);
        issuedCoupon.redeem(paymentAttemptId, orderId, LocalDateTime.now(clock));
        issuedCouponStore.save(issuedCoupon);
    }

    @Override
    public void release(Long issuedCouponId, Long paymentAttemptId) {
        if (issuedCouponId == null) {
            return;
        }
        IssuedCoupon issuedCoupon = lockIssuedCoupon(issuedCouponId);
        CouponDefinition definition = findDefinition(issuedCoupon.getDefinitionId());
        issuedCoupon.release(
                paymentAttemptId,
                definition.getValidUntil(),
                LocalDateTime.now(clock));
        issuedCouponStore.save(issuedCoupon);
    }

    @Override
    public void restoreAfterFullCancellation(Long issuedCouponId,
                                             Long orderId,
                                             LocalDateTime now) {
        if (issuedCouponId == null) {
            return;
        }
        IssuedCoupon issuedCoupon = lockIssuedCoupon(issuedCouponId);
        CouponDefinition definition = findDefinition(issuedCoupon.getDefinitionId());
        issuedCoupon.restoreAfterFullCancellation(orderId, definition.getValidUntil(), now);
        issuedCouponStore.save(issuedCoupon);
    }

    private IssuedCoupon lockIssuedCoupon(Long issuedCouponId) {
        return issuedCouponReader.findByIdForUpdate(issuedCouponId)
                .orElseThrow(NotFoundException.supplier("쿠폰"));
    }

    private CouponDefinition findDefinition(Long definitionId) {
        return definitionReader.findById(definitionId)
                .orElseThrow(NotFoundException.supplier("쿠폰 정의"));
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId < 1L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "쿠폰은 회원 주문에만 사용할 수 있습니다.");
        }
    }
}
