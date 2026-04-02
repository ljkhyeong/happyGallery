package com.personal.happygallery.app.pass.port.in;

import com.personal.happygallery.domain.pass.PassPurchase;

/**
 * Booking 도메인이 Pass 크레딧을 차감/복구할 때 사용하는 인바운드 포트.
 *
 * <p>Booking이 Pass 내부 구현(PassLedger, PassPurchaseStorePort)을
 * 직접 알지 않아도 크레딧 조작이 가능하도록 추상화한다.
 */
public interface PassCreditPort {

    /**
     * 8회권 크레딧 1회 차감.
     *
     * @param passId      8회권 ID
     * @param ownerUserId 소유자 회원 ID (회원 예약 시 non-null, 게스트 예약 시 null)
     * @return 차감된 PassPurchase
     */
    PassPurchase deductCredit(Long passId, Long ownerUserId);

    /**
     * 예약 취소 시 8회권 크레딧 1회 복구.
     *
     * @param passId    8회권 ID
     * @param bookingId 복구 사유가 된 예약 ID
     */
    void restoreCredit(Long passId, Long bookingId);
}
