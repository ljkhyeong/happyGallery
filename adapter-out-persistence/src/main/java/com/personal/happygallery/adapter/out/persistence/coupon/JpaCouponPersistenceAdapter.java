package com.personal.happygallery.adapter.out.persistence.coupon;

import com.personal.happygallery.adapter.out.persistence.support.PersistenceConstraintNames;
import com.personal.happygallery.application.coupon.port.out.CouponDefinitionStorePort;
import com.personal.happygallery.application.coupon.port.out.IssuedCouponStorePort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
class JpaCouponPersistenceAdapter
        implements CouponDefinitionStorePort, IssuedCouponStorePort {

    private static final String USER_DEFINITION_UNIQUE =
            "uq_issued_coupons_user_definition";

    private final CouponDefinitionRepository definitionRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    JpaCouponPersistenceAdapter(CouponDefinitionRepository definitionRepository,
                                IssuedCouponRepository issuedCouponRepository) {
        this.definitionRepository = definitionRepository;
        this.issuedCouponRepository = issuedCouponRepository;
    }

    @Override
    public CouponDefinition save(CouponDefinition definition) {
        return definitionRepository.saveAndFlush(definition);
    }

    @Override
    public IssuedCoupon save(IssuedCoupon issuedCoupon) {
        try {
            return issuedCouponRepository.saveAndFlush(issuedCoupon);
        } catch (DataIntegrityViolationException exception) {
            if (PersistenceConstraintNames.matches(exception, USER_DEFINITION_UNIQUE)) {
                throw new HappyGalleryException(ErrorCode.CONFLICT, "이미 발급받은 쿠폰입니다.");
            }
            throw exception;
        }
    }

    @Override
    public List<IssuedCoupon> saveAll(Iterable<IssuedCoupon> issuedCoupons) {
        return issuedCouponRepository.saveAllAndFlush(issuedCoupons);
    }

}
