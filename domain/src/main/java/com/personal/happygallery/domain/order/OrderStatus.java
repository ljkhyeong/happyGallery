package com.personal.happygallery.domain.order;

import com.personal.happygallery.common.error.AlreadyRefundedException;
import com.personal.happygallery.common.error.ProductionRefundNotAllowedException;

public enum OrderStatus {
	// 결제 및 승인
	PAID_APPROVAL_PENDING,
	APPROVED_FULFILLMENT_PENDING,
	REJECTED_REFUNDED,
	AUTO_REFUNDED_TIMEOUT,

	// 제작 및 지연
	IN_PRODUCTION,
	DELAY_REQUESTED,

	// 이행: 배송
	SHIPPING_PREPARING,
	SHIPPED,
	DELIVERED,

	// 이행: 픽업
	PICKUP_READY,
	PICKED_UP,
	PICKUP_EXPIRED_REFUNDED,

	// 최종 상태
	COMPLETED;

	/** 승인 가능한 상태인지 확인한다. 이미 환불된 경우 {@link AlreadyRefundedException}을 던진다. */
	public void requireApprovable() {
		if (this == REJECTED_REFUNDED
				|| this == AUTO_REFUNDED_TIMEOUT
				|| this == PICKUP_EXPIRED_REFUNDED) {
			throw new AlreadyRefundedException();
		}
	}

	/**
	 * 환불/취소 가능한 상태인지 확인한다.
	 * 제작 중({@link #IN_PRODUCTION}) 또는 지연 요청({@link #DELAY_REQUESTED}) 상태는
	 * {@link ProductionRefundNotAllowedException}(422)을 던진다.
	 */
	public void requireCancellable() {
		if (this == IN_PRODUCTION || this == DELAY_REQUESTED) {
			throw new ProductionRefundNotAllowedException();
		}
	}
}
