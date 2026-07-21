package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.Slot;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

/**
 * 슬롯 관리 유스케이스.
 *
 * <p>슬롯 생성·비활성화를 담당한다. 종료 시각은 클래스 소요 시간으로 계산한다.
 */
public interface SlotManagementUseCase {

    int MAX_BULK_DATE_RANGE_DAYS = 93;
    int MAX_BULK_CANDIDATES = 500;

    record BulkSlotCommand(
            Long classId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Set<DayOfWeek> weekdays,
            Set<LocalTime> startTimes
    ) {
        public BulkSlotCommand {
            weekdays = weekdays == null ? Set.of() : Set.copyOf(weekdays);
            startTimes = startTimes == null ? Set.of() : Set.copyOf(startTimes);
        }
    }

    enum BulkSlotStatus {
        CREATABLE,
        CREATED,
        SKIPPED_DUPLICATE,
        SKIPPED_PAST
    }

    record BulkSlotItem(
            Long slotId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BulkSlotStatus status,
            boolean bufferBlocked
    ) {}

    record BulkSlotResult(List<BulkSlotItem> items) {
        public BulkSlotResult {
            items = List.copyOf(items);
        }

        public long creatableCount() {
            return items.stream().filter(item -> item.status() == BulkSlotStatus.CREATABLE).count();
        }

        public long createdCount() {
            return items.stream().filter(item -> item.status() == BulkSlotStatus.CREATED).count();
        }

        public long skippedCount() {
            return items.stream()
                    .filter(item -> item.status() == BulkSlotStatus.SKIPPED_DUPLICATE
                            || item.status() == BulkSlotStatus.SKIPPED_PAST)
                    .count();
        }
    }

    Slot createSlot(Long classId, LocalDateTime startAt);

    BulkSlotResult previewBulkSlots(BulkSlotCommand command);

    BulkSlotResult createBulkSlots(BulkSlotCommand command);

    Slot deactivateSlot(Long slotId);

    Slot activateSlot(Long slotId);
}
