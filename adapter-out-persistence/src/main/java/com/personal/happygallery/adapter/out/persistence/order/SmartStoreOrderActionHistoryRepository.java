package com.personal.happygallery.adapter.out.persistence.order;

import com.personal.happygallery.application.order.port.out.SmartStoreOrderActionBacklogSummary;
import com.personal.happygallery.application.order.port.out.SmartStoreOrderActionHistoryPort;
import com.personal.happygallery.domain.order.SmartStoreOrderActionHistory;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SmartStoreOrderActionHistoryRepository
        extends JpaRepository<SmartStoreOrderActionHistory, Long>, SmartStoreOrderActionHistoryPort {

    @Override
    <S extends SmartStoreOrderActionHistory> S save(S history);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select history from SmartStoreOrderActionHistory history where history.id = :id")
    Optional<SmartStoreOrderActionHistory> findByIdWithLock(@Param("id") Long id);

    List<SmartStoreOrderActionHistory> findTop50ByProductOrderIdOrderByRequestedAtDescIdDesc(
            String productOrderId);

    @Override
    default List<SmartStoreOrderActionHistory> findRecentByProductOrderId(String productOrderId) {
        return findTop50ByProductOrderIdOrderByRequestedAtDescIdDesc(productOrderId);
    }

    @Query("""
            SELECT history
            FROM SmartStoreOrderActionHistory history
            WHERE history.reconciledAt IS NULL
              AND (history.status = com.personal.happygallery.domain.order.SmartStoreOrderActionStatus.RESULT_UNKNOWN
                   OR (history.status = com.personal.happygallery.domain.order.SmartStoreOrderActionStatus.REQUESTED
                       AND history.requestedAt <= :staleRequestedBefore))
              AND (:cursorRequestedAt IS NULL
                   OR history.requestedAt < :cursorRequestedAt
                   OR (history.requestedAt = :cursorRequestedAt AND history.id < :cursorId))
            ORDER BY history.requestedAt DESC, history.id DESC
            """)
    List<SmartStoreOrderActionHistory> queryUnresolvedPage(
            @Param("staleRequestedBefore") LocalDateTime staleRequestedBefore,
            @Param("cursorRequestedAt") LocalDateTime cursorRequestedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Override
    default List<SmartStoreOrderActionHistory> findUnresolvedPage(
            LocalDateTime staleRequestedBefore,
            LocalDateTime cursorRequestedAt,
            Long cursorId,
            int limit) {
        return queryUnresolvedPage(
                staleRequestedBefore, cursorRequestedAt, cursorId, PageRequest.of(0, limit));
    }

    @Override
    @Query("""
            SELECT new com.personal.happygallery.application.order.port.out.SmartStoreOrderActionBacklogSummary(
                COUNT(history), MIN(history.requestedAt))
            FROM SmartStoreOrderActionHistory history
            WHERE history.reconciledAt IS NULL
              AND (history.status = com.personal.happygallery.domain.order.SmartStoreOrderActionStatus.RESULT_UNKNOWN
                   OR (history.status = com.personal.happygallery.domain.order.SmartStoreOrderActionStatus.REQUESTED
                       AND history.requestedAt <= :staleRequestedBefore))
            """)
    SmartStoreOrderActionBacklogSummary summarizeUnresolvedBacklog(
            @Param("staleRequestedBefore") LocalDateTime staleRequestedBefore);
}
