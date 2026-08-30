package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotSchedulingSnapshot;
import com.personal.happygallery.domain.booking.SlotBufferPolicy;
import com.personal.happygallery.domain.booking.SlotCapacity;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.error.SlotNotAvailableException;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** 잠금 없이 현재 슬롯의 예약 가능성을 빠르게 확인한다. */
@Component
public class SlotAvailabilityChecker {

    private final SlotReaderPort slotReaderPort;
    private final Clock clock;

    public SlotAvailabilityChecker(SlotReaderPort slotReaderPort, Clock clock) {
        this.slotReaderPort = slotReaderPort;
        this.clock = clock;
    }

    /** 클래스 호환성 같은 상위 정책보다 정원 오류를 앞세우지 않는 상태 전용 확인이다. */
    SlotSchedulingSnapshot requireSchedulingAvailable(Long slotId) {
        SlotSchedulingSnapshot slot = slotReaderPort.findSchedulingSnapshotById(slotId)
                .orElseThrow(NotFoundException.supplier("슬롯"));
        if (!slot.isReservableAt(LocalDateTime.now(clock))) {
            throw new SlotNotAvailableException();
        }
        return slot;
    }

    /** 결제 준비 전에 상태, 역방향 버퍼 충돌, 요청 인원 정원을 모두 확인한다. */
    public SlotSchedulingSnapshot requireFullyAvailable(Long slotId, int participantCount) {
        SlotSchedulingSnapshot slot = requireSchedulingAvailable(slotId);
        LocalDateTime bufferWindowEnd =
                SlotBufferPolicy.bufferWindowEnd(slot.endAt(), slot.classBufferMin());
        if (slotReaderPort.countBookedConflicts(
                slot.classId(), slot.id(), slot.startAt(), bufferWindowEnd) > 0) {
            throw new SlotNotAvailableException();
        }

        SlotCapacity.checkAvailable(slot.capacity(), slot.bookedCount(), participantCount);
        return slot;
    }
}
