package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingCancellationTaskBacklogSummary;
import com.personal.happygallery.application.booking.port.out.BookingCancellationTaskPort;
import com.personal.happygallery.domain.booking.BookingCancellationTask;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
class JpaBookingCancellationTaskPersistenceAdapter implements BookingCancellationTaskPort {

    private final BookingCancellationTaskRepository repository;

    JpaBookingCancellationTaskPersistenceAdapter(BookingCancellationTaskRepository repository) {
        this.repository = repository;
    }

    @Override
    public BookingCancellationTask save(BookingCancellationTask task) {
        return repository.save(task);
    }

    @Override
    public List<BookingCancellationTask> findPending(int limit) {
        return repository.findPending(limit);
    }

    @Override
    public BookingCancellationTaskBacklogSummary summarizePendingBacklog() {
        return repository.summarizePendingBacklog();
    }

    @Override
    public Optional<BookingCancellationTask> findByIdForUpdate(Long taskId) {
        return repository.findByIdForUpdate(taskId);
    }
}
