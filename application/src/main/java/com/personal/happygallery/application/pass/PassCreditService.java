package com.personal.happygallery.application.pass;

import com.personal.happygallery.domain.pass.PassPurchase;

/**
 * Booking 흐름이 Pass 내부 저장 포트를 직접 알지 않고 8회권 크레딧을 검증·차감·복구하게 하는 내부 협력 서비스.
 */
public interface PassCreditService {

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
