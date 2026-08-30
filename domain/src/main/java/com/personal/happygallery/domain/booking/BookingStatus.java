package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public enum BookingStatus {
	BOOKED,
	CANCELED,
	NO_SHOW,
	COMPLETED;

	/** BOOKED 상태에서만 변경·취소·결석·완료 처리가 가능하다. */
	public void requireBooked() {
		if (this != BOOKED) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
					"BOOKED 상태에서만 가능합니다. 현재: " + this);
		}
	}

	/** 수강이 완료되어 클래스 후기를 작성할 수 있는지 확인한다. */
	public void requireReviewable() {
		if (!isReviewable()) {
			throw new HappyGalleryException(
					ErrorCode.REVIEW_NOT_ALLOWED,
					"완료된 예약에만 클래스 후기를 작성할 수 있습니다.");
		}
	}

	/** 수강이 완료되어 클래스 후기 작성 기회가 생긴 상태인지 반환한다. */
	public boolean isReviewable() {
		return this == COMPLETED;
	}
}
