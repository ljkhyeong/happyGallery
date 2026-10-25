package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.batch.BatchResult;

/**
 * 픽업 만료 처리 배치 유스케이스.
 *
 * <p>모든 주문을 환불 없이 {@code PICKUP_FORFEITED}로 전이한다. 기성품은 재고만 복구하고
 * 주문제작 상품은 재고도 유지한다. 관리자 예외 환불은 {@link OrderPickupUseCase#refundMissedPickup}가 맡는다.
 * 스케줄러(자동) / 관리자(수동 트리거) 두 진입점에서 호출된다.
 */
public interface PickupExpireBatchUseCase {

    BatchResult expirePickups();
}
