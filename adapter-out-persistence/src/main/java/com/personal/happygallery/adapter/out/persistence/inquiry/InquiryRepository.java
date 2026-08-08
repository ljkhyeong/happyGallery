package com.personal.happygallery.adapter.out.persistence.inquiry;

import com.personal.happygallery.application.inquiry.port.out.InquiryReaderPort;
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

public interface InquiryRepository extends JpaRepository<Inquiry, Long>, InquiryReaderPort {

    @Override Optional<Inquiry> findById(Long id);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inquiry i WHERE i.id = :id")
    Optional<Inquiry> findByIdForUpdate(@Param("id") Long id);

    List<Inquiry> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.userId = :userId
              AND (i.createdAt < :createdAt
                   OR (i.createdAt = :createdAt AND i.id < :id))
            ORDER BY i.createdAt DESC, i.id DESC
            """)
    List<Inquiry> findByUserIdAfterPage(
            @Param("userId") Long userId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

    List<Inquiry> findAllByOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.createdAt < :createdAt
               OR (i.createdAt = :createdAt AND i.id < :id)
            ORDER BY i.createdAt DESC, i.id DESC
            """)
    List<Inquiry> findRecentAfterPage(@Param("createdAt") LocalDateTime createdAt,
                                      @Param("id") Long id,
                                      Pageable pageable);

    @Override
    default List<Inquiry> findByUserId(Long userId, int limit) {
        return findByUserIdOrderByCreatedAtDescIdDesc(
                userId, PageRequest.ofSize(limit));
    }

    @Override
    default List<Inquiry> findByUserIdAfter(
            Long userId, LocalDateTime createdAt, Long id, int limit) {
        return findByUserIdAfterPage(
                userId, createdAt, id, PageRequest.ofSize(limit));
    }

    @Override
    default List<Inquiry> findRecent(int limit) {
        return findAllByOrderByCreatedAtDescIdDesc(PageRequest.ofSize(limit));
    }

    @Override
    default List<Inquiry> findRecentAfter(LocalDateTime createdAt, Long id, int limit) {
        return findRecentAfterPage(createdAt, id, PageRequest.ofSize(limit));
    }
}
