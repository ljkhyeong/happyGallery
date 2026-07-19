package com.personal.happygallery.application.pass.port.in;

import com.personal.happygallery.domain.pass.PassPurchase;

/**
 * 8회권 구매 유스케이스 (회원 전용).
 *
 * <p>prepare 단계가 서버 설정({@code app.pass.total-price} / {@code PASS_TOTAL_PRICE} env)으로 가격을 확정하고,
 * 구매 단계는 그 스냅샷을 저장한다. 클라이언트가 금액을 보내지 않는다.
 */
public interface PassPurchaseUseCase {

    PassPurchase purchaseForMember(Long userId, long preparedTotalPrice);
}
