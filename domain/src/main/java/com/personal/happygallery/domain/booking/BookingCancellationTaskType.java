package com.personal.happygallery.domain.booking;

/** 공방 사정으로 예약을 취소한 뒤 운영자가 직접 마무리해야 하는 작업 유형. */
public enum BookingCancellationTaskType {
    BALANCE_SETTLEMENT,
    MANUAL_COMPENSATION
}
