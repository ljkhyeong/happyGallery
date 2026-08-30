package com.personal.happygallery.application.coupon.port.out;

import com.personal.happygallery.domain.coupon.CouponDefinition;

public interface CouponDefinitionStorePort {

    <S extends CouponDefinition> S saveAndFlush(S definition);
}
