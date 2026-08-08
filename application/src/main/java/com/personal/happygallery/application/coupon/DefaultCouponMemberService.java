package com.personal.happygallery.application.coupon;

import com.personal.happygallery.application.coupon.port.in.CouponMemberUseCase;
import com.personal.happygallery.application.coupon.port.out.CouponDefinitionReaderPort;
import com.personal.happygallery.application.coupon.port.out.IssuedCouponReaderPort;
import com.personal.happygallery.application.coupon.port.out.IssuedCouponStorePort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.coupon.IssuedCouponStatus;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Service
public class DefaultCouponMemberService implements CouponMemberUseCase {

    private final CouponDefinitionReaderPort definitionReader;
    private final IssuedCouponReaderPort issuedCouponReader;
    private final IssuedCouponStorePort issuedCouponStore;
    private final Clock clock;

    public DefaultCouponMemberService(CouponDefinitionReaderPort definitionReader,
                                      IssuedCouponReaderPort issuedCouponReader,
                                      IssuedCouponStorePort issuedCouponStore,
                                      Clock clock) {
        this.definitionReader = definitionReader;
        this.issuedCouponReader = issuedCouponReader;
        this.issuedCouponStore = issuedCouponStore;
        this.clock = clock;
    }

    @Override
    @Transactional
    public IssuedCouponView claim(Long userId, Long definitionId) {
        requireUserId(userId);
        CouponDefinition definition = definitionReader.findByIdForUpdate(definitionId)
                .orElseThrow(NotFoundException.supplier("쿠폰 정의"));
        LocalDateTime now = LocalDateTime.now(clock);
        definition.requirePubliclyClaimableAt(now);
        if (issuedCouponReader.findByUserIdAndDefinitionId(userId, definitionId).isPresent()) {
            throw alreadyClaimed();
        }
        IssuedCoupon issuedCoupon = issuedCouponStore.save(
                new IssuedCoupon(definitionId, userId, now));
        return new IssuedCouponView(issuedCoupon, definition);
    }

    @Override
    @Transactional
    public List<CouponDefinition> listClaimableCoupons(Long userId) {
        requireUserId(userId);
        LocalDateTime now = LocalDateTime.now(clock);
        var claimedDefinitionIds = issuedCouponReader
                .findByUserIdOrderByClaimedAtDescIdDesc(userId).stream()
                .map(IssuedCoupon::getDefinitionId)
                .collect(toSet());
        return definitionReader.findAllByOrderByIdDesc().stream()
                .filter(definition -> definition.isPubliclyClaimableAt(now))
                .filter(definition -> !claimedDefinitionIds.contains(definition.getId()))
                .toList();
    }

    @Override
    @Transactional
    public List<IssuedCouponView> listMyCoupons(Long userId) {
        requireUserId(userId);
        List<IssuedCoupon> issuedCoupons =
                issuedCouponReader.findTop100ByUserIdOrderByClaimedAtDescIdDesc(userId);
        if (issuedCoupons.isEmpty()) {
            return List.of();
        }
        List<Long> definitionIds = issuedCoupons.stream()
                .map(IssuedCoupon::getDefinitionId)
                .distinct()
                .toList();
        Map<Long, CouponDefinition> definitions = definitionReader.findAllById(definitionIds)
                .stream()
                .collect(toMap(CouponDefinition::getId, Function.identity()));

        LocalDateTime now = LocalDateTime.now(clock);
        List<IssuedCoupon> changed = new ArrayList<>();
        for (IssuedCoupon issued : issuedCoupons) {
            CouponDefinition definition = requireDefinition(definitions, issued);
            if (!definition.isActive()
                    && issued.getStatus() == IssuedCouponStatus.AVAILABLE) {
                issued.cancel();
                changed.add(issued);
            } else if (issued.expireIfReached(definition.getValidUntil(), now)) {
                changed.add(issued);
            }
        }
        if (!changed.isEmpty()) {
            issuedCouponStore.saveAll(changed);
        }
        return issuedCoupons.stream()
                .map(issued -> new IssuedCouponView(
                        issued,
                        requireDefinition(definitions, issued)))
                .toList();
    }

    private static CouponDefinition requireDefinition(Map<Long, CouponDefinition> definitions,
                                                       IssuedCoupon issuedCoupon) {
        CouponDefinition definition = definitions.get(issuedCoupon.getDefinitionId());
        if (definition == null) {
            throw new NotFoundException("쿠폰 정의");
        }
        return definition;
    }

    private static void requireUserId(Long userId) {
        if (userId == null || userId < 1L) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "쿠폰 회원 정보가 누락되었습니다.");
        }
    }

    private static HappyGalleryException alreadyClaimed() {
        return new HappyGalleryException(ErrorCode.CONFLICT, "이미 발급받은 쿠폰입니다.");
    }
}
