package com.personal.happygallery.domain.order;

import com.personal.happygallery.domain.error.AlreadyRefundedException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.ProductionRefundNotAllowedException;

public enum OrderStatus {
	// 결제 및 승인
	PAID_APPROVAL_PENDING,
	APPROVED_FULFILLMENT_PENDING,
	REJECTED,
	CUSTOMER_CANCELED,
	AUTO_REFUND_TIMEOUT,

	// 제작 및 지연
	IN_PRODUCTION,
	DELAY_CONSENT_PENDING,
	DELAY_ACCEPTED,
	DELAY_REJECTED_CANCELED,

	// 이행: 배송
	SHIPPING_PREPARING,
	SHIPPED,
	DELIVERED,

	// 이행: 픽업
	PICKUP_READY,
	PICKED_UP,
	PICKUP_EXPIRED,
	PICKUP_FORFEITED,

	// 최종 상태
	COMPLETED;

	/**
	 * 관리자 승인/거절이 가능한 승인 대기 상태인지 확인한다.
	 * 이미 환불된 주문은 {@link AlreadyRefundedException}을 던지고,
	 * 그 외 승인 대기 외 상태는 {@code 400 INVALID_INPUT}을 던진다.
	 */
	public void requireApprovalPending() {
		if (this == REJECTED
				|| this == CUSTOMER_CANCELED
				|| this == AUTO_REFUND_TIMEOUT
				|| this == PICKUP_EXPIRED
				|| this == DELAY_REJECTED_CANCELED) {
			throw new AlreadyRefundedException();
		}
		if (this != PAID_APPROVAL_PENDING) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "승인 대기 상태의 주문만 처리할 수 있습니다.");
		}
	}

	/**
	 * 환불/취소 가능한 상태인지 확인한다.
	 * 제작 중({@link #IN_PRODUCTION}) 또는 지연 수락({@link #DELAY_ACCEPTED}) 상태는
	 * {@link ProductionRefundNotAllowedException}(422)을 던진다.
	 */
	public void requireCancellable() {
		if (this == IN_PRODUCTION || this == DELAY_CONSENT_PENDING || this == DELAY_ACCEPTED) {
			throw new ProductionRefundNotAllowedException();
		}
	}

	/** 고객이 결제 승인 전 주문을 직접 취소할 수 있는지 확인한다. */
	public void requireCustomerCancellationAllowed() {
		if (this == REJECTED
				|| this == CUSTOMER_CANCELED
				|| this == AUTO_REFUND_TIMEOUT
				|| this == PICKUP_EXPIRED
				|| this == DELAY_REJECTED_CANCELED) {
			throw new AlreadyRefundedException();
		}
		if (this != PAID_APPROVAL_PENDING) {
			throw new HappyGalleryException(
					ErrorCode.INVALID_INPUT, "승인 대기 상태의 주문만 고객이 취소할 수 있습니다.");
		}
	}

	/** {@link #IN_PRODUCTION} 상태인지 확인한다. */
	public void requireInProduction() {
		if (this != IN_PRODUCTION) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "제작 중 상태에서만 가능합니다.");
		}
	}

	/** {@link #DELAY_ACCEPTED} 상태인지 확인한다. */
	public void requireDelayAccepted() {
		if (this != DELAY_ACCEPTED) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "지연 수락 상태에서만 가능합니다.");
		}
	}

	/** {@link #DELAY_CONSENT_PENDING} 상태인지 확인한다. */
	public void requireDelayConsentPending() {
		if (this != DELAY_CONSENT_PENDING) {
			throw new HappyGalleryException(
					ErrorCode.INVALID_INPUT, "지연 동의 대기 상태에서만 응답할 수 있습니다.");
		}
	}

	/** 고객이 제작 지연을 거절해 취소할 수 있는 상태인지 확인한다. */
	public void requireDelayRejectionCancelable() {
		if (this != DELAY_CONSENT_PENDING) {
			throw new HappyGalleryException(
					ErrorCode.INVALID_INPUT, "지연 동의 대기 상태에서만 지연 거절 취소가 가능합니다.");
		}
	}

	/** {@link #IN_PRODUCTION} 또는 {@link #DELAY_ACCEPTED} 상태인지 확인한다. */
	public void requireProductionCompletable() {
		if (this != IN_PRODUCTION && this != DELAY_ACCEPTED) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "제작 중이거나 지연 수락 상태에서만 완료할 수 있습니다.");
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
		if (this != IN_PRODUCTION
				&& this != DELAY_CONSENT_PENDING
				&& this != DELAY_ACCEPTED
				&& this != SHIPPING_PREPARING) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
					"제작 중, 지연 응답 대기, 지연 수락, 배송 준비 상태에서만 출고일을 설정할 수 있습니다.");
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
