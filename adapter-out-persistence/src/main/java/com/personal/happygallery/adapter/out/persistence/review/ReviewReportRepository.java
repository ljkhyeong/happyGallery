package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReviewReport r WHERE r.id = :reportId")
    Optional<ReviewReport> findByIdForUpdate(@Param("reportId") Long reportId);

    boolean existsByReviewIdAndReporterUserId(Long reviewId, Long reporterUserId);

    @Query("""
            SELECT r FROM ReviewReport r
            WHERE (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewReport> findAdminRows(
            @Param("status") ReviewReportStatus status, Pageable pageable);

    @Query("""
            SELECT r FROM ReviewReport r
            WHERE (:status IS NULL OR r.status = :status)
              AND (r.createdAt < :createdAt
                   OR (r.createdAt = :createdAt AND r.id < :id))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewReport> findAdminRowsAfter(
            @Param("status") ReviewReportStatus status,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

    @Query("""
            SELECT r.reviewId FROM ReviewReport r
            WHERE r.reporterUserId = :userId
              AND r.reviewId IN :reviewIds
            """)
    List<Long> findReportedReviewIds(
            @Param("userId") Long userId, @Param("reviewIds") List<Long> reviewIds);
}
