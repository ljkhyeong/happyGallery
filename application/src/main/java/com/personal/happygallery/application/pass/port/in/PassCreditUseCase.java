package com.personal.happygallery.application.pass.port.in;

import com.personal.happygallery.domain.pass.PassPurchase;

/**
 * Booking 도메인이 Pass 크레딧을 차감/복구할 때 사용하는 인바운드 포트.
 *
 * <p>Booking이 Pass 내부 구현(PassLedger, PassPurchaseStorePort)을
 * 직접 알지 않아도 크레딧 조작이 가능하도록 추상화한다.
 */
public interface PassCreditUseCase {

    /**
     * 8회권 사용 가능 여부를 검증하고 예약 생성에 연결할 8회권을 반환한다.
     *
     * @param passId      8회권 ID
     * @param ownerUserId 소유자 회원 ID (회원 예약 시 non-null, 게스트 예약 시 null)
     * @return 사용 가능한 PassPurchase
     */
    PassPurchase requireUsable(Long passId, Long ownerUserId);

    /**
     * 예약 생성 완료 후 8회권 크레딧 1회를 차감한다.
     *
     * @param passId      8회권 ID
     * @param ownerUserId 소유자 회원 ID (회원 예약 시 non-null, 게스트 예약 시 null)
     * @param bookingId   차감 사유가 된 예약 ID
     * @return 차감된 PassPurchase
     */
    PassPurchase deductCredit(Long passId, Long ownerUserId, Long bookingId);

    /**
     * 예약 취소 시 8회권 크레딧 1회 복구.
     *
     * @param passId    8회권 ID
     * @param bookingId 복구 사유가 된 예약 ID
     */
    void restoreCredit(Long passId, Long bookingId);
}
