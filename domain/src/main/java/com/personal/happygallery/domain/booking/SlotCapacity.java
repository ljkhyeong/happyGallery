package com.personal.happygallery.domain.booking;

import com.personal.happygallery.common.error.CapacityExceededException;

/** 슬롯 정원 정책. 슬롯당 최대 예약 인원을 관리한다. */
public final class SlotCapacity {

    public static final int MAX = 8;

    private SlotCapacity() {}

    /** 현재 예약 수가 정원 미만인지 확인한다. 정원 초과 시 {@link CapacityExceededException}을 던진다. */
    public static void checkAvailable(int bookedCount) {
        if (bookedCount >= MAX) {
            throw new CapacityExceededException();
        }
    }
}
