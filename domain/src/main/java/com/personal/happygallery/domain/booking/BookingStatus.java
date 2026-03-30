package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

public enum BookingStatus {
	BOOKED,
	CANCELED,
	NO_SHOW,
	COMPLETED;

	/** BOOKED 상태에서만 변경·취소·결석 처리가 가능하다. */
	public void requireBooked() {
		if (this != BOOKED) {
			throw new HappyGalleryException(ErrorCode.INVALID_INPUT,
					"BOOKED 상태에서만 가능합니다. 현재: " + this);
		}
	}
}
