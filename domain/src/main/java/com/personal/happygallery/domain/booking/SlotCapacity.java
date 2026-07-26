package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.CapacityExceededException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

/** 슬롯 정원 정책. 슬롯당 최대 예약 인원을 관리한다. */
public final class SlotCapacity {

    public static final int MAX = 8;

    private SlotCapacity() {}

    /** 현재 예약 수가 정원 미만인지 확인한다. 정원 초과 시 {@link CapacityExceededException}을 던진다. */
    public static void checkAvailable(int bookedCount) {
        checkAvailable(bookedCount, 1);
    }

    /** 요청 인원 전체를 수용할 수 있는지 확인한다. */
    public static void checkAvailable(int bookedCount, int participantCount) {
        requireValidParticipantCount(participantCount);
        if (bookedCount < 0 || bookedCount > MAX - participantCount) {
            throw new CapacityExceededException();
        }
    }

    public static void requireValidParticipantCount(int participantCount) {
        if (participantCount < 1 || participantCount > MAX) {
            throw new HappyGalleryException(
                    ErrorCode.INVALID_INPUT,
                    "예약 인원은 1명 이상 " + MAX + "명 이하여야 합니다.");
        }
    }
}
