package com.personal.happygallery.application.booking.port.out;

import com.personal.happygallery.domain.booking.BookingCancellationTask;
import java.util.List;
import java.util.Optional;

public interface BookingCancellationTaskPort {

    BookingCancellationTask save(BookingCancellationTask task);

    List<BookingCancellationTask> findPending(int limit);

    Optional<BookingCancellationTask> findByIdForUpdate(Long taskId);
}
