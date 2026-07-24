package com.personal.happygallery.application.booking.port.in;

import com.personal.happygallery.domain.booking.BookingCancellationTaskStatus;
import com.personal.happygallery.domain.booking.BookingCancellationTaskType;
import java.time.LocalDateTime;
import java.util.List;

/** 공방 사정 예약 취소 후속 작업을 조회하고 완료하는 관리자 유스케이스. */
public interface BookingCancellationTaskUseCase {

    List<TaskView> listPending();

    CompletionResult complete(Long taskId, Long adminId);

    record TaskView(
            Long taskId,
            Long bookingId,
            String bookingNumber,
            BookingCancellationTaskType type,
            BookingCancellationTaskStatus status,
            String className,
            LocalDateTime startAt,
            long balanceAmount,
            String reason,
            LocalDateTime createdAt,
            Long completedByAdminId,
            LocalDateTime completedAt
    ) {}

    record CompletionResult(TaskView task, boolean changed) {}
}
