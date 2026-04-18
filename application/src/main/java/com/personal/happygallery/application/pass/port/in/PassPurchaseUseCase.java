package com.personal.happygallery.application.pass.port.in;

import com.personal.happygallery.domain.pass.PassPurchase;

/** 8회권 구매 유스케이스 (회원 전용). */
public interface PassPurchaseUseCase {

    PassPurchase purchaseForMember(Long userId, long totalPrice);
}
