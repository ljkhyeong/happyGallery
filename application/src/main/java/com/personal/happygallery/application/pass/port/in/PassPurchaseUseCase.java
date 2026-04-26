package com.personal.happygallery.application.pass.port.in;

import com.personal.happygallery.domain.pass.PassPurchase;

/**
 * 8회권 구매 유스케이스 (회원 전용).
 *
 * <p>가격은 서버 설정({@code app.pass.total-price} / {@code PASS_TOTAL_PRICE} env)에서 주입되므로
 * 클라이언트가 금액을 보내지 않는다.
 */
public interface PassPurchaseUseCase {

    PassPurchase purchaseForMember(Long userId);
}
