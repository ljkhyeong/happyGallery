package com.personal.happygallery.application.pass;

import com.personal.happygallery.domain.pass.PassPurchase;

/**
 * Booking 흐름이 Pass 내부 저장 포트를 직접 알지 않고 8회권 크레딧을 검증·차감·복구하게 하는 내부 협력 서비스.
 */
public interface PassCreditService {

    /** 8회권 행을 잠그고 회원 소유권을 확인한다. 슬롯 잠금보다 먼저 호출해야 한다. */
    PassPurchase requireOwnedForUpdate(Long passId, Long ownerUserId);

    /** 8회권 행을 잠근다. 슬롯 잠금보다 먼저 호출해야 한다. */
    PassPurchase requireForUpdate(Long passId);

    /**
     * 예약 생성 완료 후 8회권 크레딧 1회를 차감한다.
     *
     * @param pass      예약에 연결한 8회권
     * @param bookingId 차감 사유가 된 예약 ID
     * @return 차감된 PassPurchase
     */
    PassPurchase deductCredit(PassPurchase pass, Long bookingId);

    /**
     * 예약 취소 시 8회권 크레딧 1회 복구.
     *
     * @param pass      잠금이 확보된 8회권
     * @param bookingId 복구 사유가 된 예약 ID
     */
    void restoreCredit(PassPurchase pass, Long bookingId);
}
