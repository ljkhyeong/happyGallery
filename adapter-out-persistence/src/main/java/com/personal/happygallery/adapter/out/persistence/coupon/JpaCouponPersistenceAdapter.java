package com.personal.happygallery.adapter.out.persistence.coupon;

import com.personal.happygallery.application.coupon.port.out.CouponDefinitionStorePort;
import com.personal.happygallery.application.coupon.port.out.IssuedCouponStorePort;
import com.personal.happygallery.domain.coupon.CouponDefinition;
import com.personal.happygallery.domain.coupon.IssuedCoupon;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.util.List;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

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
            if (hasConstraint(exception, USER_DEFINITION_UNIQUE)) {
                throw new HappyGalleryException(ErrorCode.CONFLICT, "이미 발급받은 쿠폰입니다.");
            }
            throw exception;
        }
    }

    @Override
    public List<IssuedCoupon> saveAll(Iterable<IssuedCoupon> issuedCoupons) {
        return issuedCouponRepository.saveAllAndFlush(issuedCoupons);
    }

    private static boolean hasConstraint(Throwable throwable, String expectedConstraint) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && StringUtils.hasText(violation.getConstraintName())) {
                String constraint = StringUtils.unqualify(violation.getConstraintName()
                        .toLowerCase(Locale.ROOT)
                        .replace("`", "")
                        .replace("\"", "")
                        .replace("'", ""));
                return expectedConstraint.equals(constraint);
            }
            current = current.getCause();
        }
        return false;
    }
}
