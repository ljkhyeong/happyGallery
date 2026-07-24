package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase;
import com.personal.happygallery.application.booking.port.in.BookingCancellationTaskUseCase.TaskView;
import com.personal.happygallery.application.booking.port.out.BookingCancellationTaskPort;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.booking.BookingCancellationTask;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultBookingCancellationTaskService implements BookingCancellationTaskUseCase {

    private static final int LIST_LIMIT = 100;

    private final BookingCancellationTaskPort taskPort;
    private final Clock clock;

    public DefaultBookingCancellationTaskService(
            BookingCancellationTaskPort taskPort,
            Clock clock
    ) {
        this.taskPort = taskPort;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskView> listPending() {
        return taskPort.findPending(LIST_LIMIT).stream()
                .map(DefaultBookingCancellationTaskService::toTaskView)
                .toList();
    }

    @Override
    @Transactional
    public CompletionResult complete(Long taskId, Long adminId) {
        BookingCancellationTask task = taskPort.findByIdForUpdate(taskId)
                .orElseThrow(NotFoundException.supplier("예약 취소 후속 작업"));
        boolean changed = task.complete(adminId, LocalDateTime.now(clock));
        if (changed) {
            taskPort.save(task);
        }
        return new CompletionResult(toTaskView(task), changed);
    }

    private static TaskView toTaskView(BookingCancellationTask task) {
        Booking booking = task.getBooking();
        return new TaskView(
                task.getId(),
                booking.getId(),
                "BK-%08d".formatted(booking.getId()),
                task.getType(),
                task.getStatus(),
                booking.getBookingClass().getName(),
                booking.getSlot().getStartAt(),
                booking.getBalanceAmount(),
                task.getReason(),
                task.getCreatedAt(),
                task.getCompletedByAdminId(),
                task.getCompletedAt());
    }
}
