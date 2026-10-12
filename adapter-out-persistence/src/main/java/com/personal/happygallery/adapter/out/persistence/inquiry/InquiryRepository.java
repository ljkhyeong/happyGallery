package com.personal.happygallery.adapter.out.persistence.inquiry;

import com.personal.happygallery.application.inquiry.port.out.InquiryReaderPort;
import com.personal.happygallery.application.inquiry.port.out.InquiryStorePort;
import com.personal.happygallery.domain.inquiry.Inquiry;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface InquiryRepository extends JpaRepository<Inquiry, Long>,
        InquiryReaderPort,
        InquiryStorePort {

    @Override
    <S extends Inquiry> S save(S inquiry);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inquiry i WHERE i.id = :id")
    Optional<Inquiry> findByIdForUpdate(@Param("id") Long id);

    @Override
    Optional<Inquiry> findByIdAndUserId(Long id, Long userId);

    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.userId = :userId
              AND (:cursorCreatedAt IS NULL
                   OR i.createdAt < :cursorCreatedAt
                   OR (i.createdAt = :cursorCreatedAt AND i.id < :cursorId))
            ORDER BY i.createdAt DESC, i.id DESC
            """)
    List<Inquiry> findPageByUserId(
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("""
            SELECT i FROM Inquiry i
            WHERE :cursorCreatedAt IS NULL
               OR i.createdAt < :cursorCreatedAt
               OR (i.createdAt = :cursorCreatedAt AND i.id < :cursorId)
            ORDER BY i.createdAt DESC, i.id DESC
            """)
    List<Inquiry> findRecentPage(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Override
    default List<Inquiry> findByUserId(
            Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findPageByUserId(userId, cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }

    @Override
    default List<Inquiry> findRecent(LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findRecentPage(cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }
}
