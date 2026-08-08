package com.personal.happygallery.application.coupon.port.in;

import com.personal.happygallery.domain.coupon.CouponDefinition;
import java.util.List;

/** 관리자 쿠폰 정의 CRUD 유스케이스. */
public interface CouponAdminUseCase {

    List<CouponDefinition> list();

    CouponDefinition get(Long definitionId);

    CouponDefinition create(CouponDefinitionCommand command);

    CouponDefinition update(Long definitionId, long expectedVersion, CouponDefinitionCommand command);

    void delete(Long definitionId, long expectedVersion);
}
