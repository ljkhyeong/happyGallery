package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.BookingCancellationTaskBacklogSummary;
import com.personal.happygallery.application.booking.port.out.BookingCancellationTaskPort;
import com.personal.happygallery.domain.booking.BookingCancellationTask;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingCancellationTaskRepository
        extends JpaRepository<BookingCancellationTask, Long>, BookingCancellationTaskPort {

    @Override
    <S extends BookingCancellationTask> S save(S task);

    @Query("""
            SELECT task
            FROM BookingCancellationTask task
            JOIN FETCH task.booking booking
            JOIN FETCH booking.bookingClass
            JOIN FETCH booking.slot
            WHERE task.status =
                com.personal.happygallery.domain.booking.BookingCancellationTaskStatus.PENDING
            ORDER BY task.createdAt, task.id
            """)
    List<BookingCancellationTask> findPendingPage(Pageable pageable);

    @Override
    default List<BookingCancellationTask> findPending(int limit) {
        return findPendingPage(PageRequest.ofSize(limit));
    }

    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.booking.port.out.BookingCancellationTaskBacklogSummary(
                COUNT(task),
                MIN(task.createdAt)
            )
            FROM BookingCancellationTask task
            WHERE task.status =
                com.personal.happygallery.domain.booking.BookingCancellationTaskStatus.PENDING
            """)
    BookingCancellationTaskBacklogSummary summarizePendingBacklog();

    @Query("""
            SELECT CASE WHEN COUNT(task) > 0 THEN true ELSE false END
            FROM BookingCancellationTask task
            WHERE task.status =
                com.personal.happygallery.domain.booking.BookingCancellationTaskStatus.PENDING
              AND task.booking.userId = :userId
            """)
    boolean existsPendingByUserId(@Param("userId") Long userId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT task
            FROM BookingCancellationTask task
            JOIN FETCH task.booking booking
            JOIN FETCH booking.bookingClass
            JOIN FETCH booking.slot
            WHERE task.id = :taskId
            """)
    Optional<BookingCancellationTask> findByIdForUpdate(@Param("taskId") Long taskId);
}
