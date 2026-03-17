package com.personal.happygallery.app.pass.port.in;

import com.personal.happygallery.app.batch.BatchResult;

/**
 * 8회권 만료 배치 유스케이스.
 *
 * <p>스케줄러(자동) / 관리자(수동 트리거) 두 진입점에서 호출된다.
 */
public interface PassExpiryBatchUseCase {

    BatchResult expireAll();

    BatchResult sendExpiryNotifications();
}
