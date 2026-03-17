package com.personal.happygallery.app.pass.port.in;

import com.personal.happygallery.domain.pass.PassPurchase;

/**
 * 8회권 구매 유스케이스.
 *
 * <p>게스트(직접/전화 인증) / 회원 세 경로를 지원한다.
 */
public interface PassPurchaseUseCase {

    PassPurchase purchaseForGuest(Long guestId, long totalPrice);

    PassPurchase purchaseByPhone(String phone, String verificationCode, String name, long totalPrice);

    PassPurchase purchaseForMember(Long userId, long totalPrice);
}
