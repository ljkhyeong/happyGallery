package com.personal.happygallery.adapter.out.persistence.inquiry;

import com.personal.happygallery.application.inquiry.port.out.GroupInquiryPort;
import com.personal.happygallery.domain.inquiry.GroupInquiry;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface GroupInquiryRepository extends JpaRepository<GroupInquiry, Long>, GroupInquiryPort {
    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT i FROM GroupInquiry i WHERE i.id = :id")
    Optional<GroupInquiry> findByIdForUpdate(Long id);

    @Query("""
            SELECT i FROM GroupInquiry i
            WHERE (:userId IS NULL OR i.userId = :userId)
              AND (:status IS NULL OR i.status = :status)
              AND (:before IS NULL OR i.createdAt < :before OR (i.createdAt = :before AND i.id < :beforeId))
            ORDER BY i.createdAt DESC, i.id DESC
            """)
    List<GroupInquiry> searchPage(Long userId, GroupInquiryStatus status, LocalDateTime before, Long beforeId, Pageable pageable);

    @Override
    default List<GroupInquiry> search(Long userId, GroupInquiryStatus status, LocalDateTime before, Long beforeId, int limit) {
        return searchPage(userId, status, before, beforeId, PageRequest.ofSize(limit));
    }

    @Query("""
            SELECT i FROM GroupInquiry i
            WHERE i.nextContactOn <= :today
              AND i.status <> com.personal.happygallery.domain.inquiry.GroupInquiryStatus.CLOSED
              AND (:after IS NULL OR i.nextContactOn > :after OR (i.nextContactOn = :after AND i.id > :afterId))
            ORDER BY i.nextContactOn, i.id
            """)
    List<GroupInquiry> followUpPage(LocalDate today, LocalDate after, Long afterId, Pageable pageable);

    @Override
    default List<GroupInquiry> findFollowUps(LocalDate today, LocalDate after, Long afterId, int limit) {
        return followUpPage(today, after, afterId, PageRequest.ofSize(limit));
    }

    @Override
    @Modifying
    @Query("DELETE FROM GroupInquiry i WHERE i.userId = :userId")
    void deleteByUserId(Long userId);
}
