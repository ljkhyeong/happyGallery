package com.personal.happygallery.application.order.port.in;

import com.personal.happygallery.application.batch.BatchResult;

/**
 * 픽업 만료 처리 배치 유스케이스.
 *
 * <p>기성품은 환불·재고 복구하고 주문제작 상품은 환불 없이 만료한다.
 * 스케줄러(자동) / 관리자(수동 트리거) 두 진입점에서 호출된다.
 */
public interface PickupExpireBatchUseCase {

    BatchResult expirePickups();
}
