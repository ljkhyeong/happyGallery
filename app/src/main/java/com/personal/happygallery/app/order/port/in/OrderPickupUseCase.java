package com.personal.happygallery.app.order.port.in;

import com.personal.happygallery.app.order.OrderPickupService.PickupResult;
import java.time.LocalDateTime;

/**
 * 픽업 이행 관리 유스케이스.
 *
 * <p>픽업 준비 완료와 픽업 완료 처리를 지원한다.
 */
public interface OrderPickupUseCase {

    PickupResult markPickupReady(Long orderId, LocalDateTime pickupDeadlineAt);

    PickupResult confirmPickup(Long orderId);
}
