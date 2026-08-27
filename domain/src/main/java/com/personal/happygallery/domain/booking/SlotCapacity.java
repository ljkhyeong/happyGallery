package com.personal.happygallery.domain.booking;

import com.personal.happygallery.domain.error.CapacityExceededException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;

/** 슬롯 정원 정책. 클래스별 슬롯 정원 안에서 예약 인원을 관리한다. */
public final class SlotCapacity {

    public static final int DEFAULT = 8;

    private SlotCapacity() {}

    /** 요청 인원 전체를 수용할 수 있는지 확인한다. */
    public static void checkAvailable(int capacity, int bookedCount, int participantCount) {
        requireValidCapacity(capacity);
        requireValidParticipantCount(participantCount);
        if (bookedCount < 0 || bookedCount > capacity - participantCount) {
            throw new CapacityExceededException();
        }
    }

    public static void requireValidCapacity(int capacity) {
        if (capacity < 1) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "클래스 정원은 1명 이상이어야 합니다.");
        }
    }

    public static void requireValidParticipantCount(int participantCount) {
        if (participantCount < 1) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "예약 인원은 1명 이상이어야 합니다.");
        }
    }
}
