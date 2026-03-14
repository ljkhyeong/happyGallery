package com.personal.happygallery.domain.order;

import com.personal.happygallery.common.error.AlreadyRefundedException;
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.common.error.ProductionRefundNotAllowedException;

public enum OrderStatus {
	// 결제 및 승인
	PAID_APPROVAL_PENDING,
	APPROVED_FULFILLMENT_PENDING,
	REJECTED,
	AUTO_REFUND_TIMEOUT,

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
	PICKUP_EXPIRED,

	// 최종 상태
	COMPLETED;

	/** 승인 가능한 상태인지 확인한다. 이미 환불된 경우 {@link AlreadyRefundedException}을 던진다. */
	public void requireApprovable() {
		if (this == REJECTED
				|| this == AUTO_REFUND_TIMEOUT
				|| this == PICKUP_EXPIRED) {
			throw new AlreadyRefundedException();
		}
	}

	/**
	 * 관리자 승인/거절이 가능한 승인 대기 상태인지 확인한다.
	 * 이미 환불된 주문은 {@link AlreadyRefundedException}을 던지고,
	 * 그 외 승인 대기 외 상태는 {@code 400 INVALID_INPUT}을 던진다.
	 */
	public void requireApprovalPending() {
		requireApprovable();
		if (this != PAID_APPROVAL_PENDING) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "승인 대기 상태의 주문만 처리할 수 있습니다.");
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

	/** {@link #IN_PRODUCTION} 상태인지 확인한다. */
	public void requireInProduction() {
		if (this != IN_PRODUCTION) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "제작 중 상태에서만 가능합니다.");
		}
	}

	/** {@link #DELAY_REQUESTED} 상태인지 확인한다. */
	public void requireDelayRequested() {
		if (this != DELAY_REQUESTED) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지연 요청 상태에서만 가능합니다.");
		}
	}

	/** {@link #IN_PRODUCTION} 또는 {@link #DELAY_REQUESTED} 상태인지 확인한다. */
	public void requireProductionCompletable() {
		if (this != IN_PRODUCTION && this != DELAY_REQUESTED) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "제작 중이거나 지연 요청 상태에서만 완료할 수 있습니다.");
		}
	}

	/** {@link #APPROVED_FULFILLMENT_PENDING} 상태인지 확인한다. */
	public void requireFulfillmentPending() {
		if (this != APPROVED_FULFILLMENT_PENDING) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이행 대기 상태에서만 가능합니다.");
		}
	}

	/** {@link #PICKUP_READY} 상태인지 확인한다. */
	public void requirePickupReady() {
		if (this != PICKUP_READY) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "픽업 준비 상태에서만 가능합니다.");
		}
	}

	/** expectedShipDate 갱신이 허용되는 상태인지 확인한다 (제작 중/지연/배송 준비). */
	public void requireExpectedShipDateWritable() {
		if (this != IN_PRODUCTION && this != DELAY_REQUESTED && this != SHIPPING_PREPARING) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
					"제작 중, 지연 요청, 배송 준비 상태에서만 출고일을 설정할 수 있습니다.");
		}
	}

	/** {@link #APPROVED_FULFILLMENT_PENDING} 상태에서 배송 준비로 전환 가능한지 확인한다. */
	public void requireShippingPreparable() {
		if (this != APPROVED_FULFILLMENT_PENDING) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이행 대기 상태에서만 배송 준비가 가능합니다.");
		}
	}

	/** {@link #SHIPPING_PREPARING} 상태인지 확인한다. */
	public void requireShippingPreparing() {
		if (this != SHIPPING_PREPARING) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "배송 준비 상태에서만 가능합니다.");
		}
	}

	/** {@link #SHIPPED} 상태인지 확인한다. */
	public void requireShipped() {
		if (this != SHIPPED) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "배송 중 상태에서만 가능합니다.");
		}
	}
}
