package com.personal.happygallery.adapter.out.persistence.review;

import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportStatus;
import com.personal.happygallery.application.review.port.out.ReviewReportListView;
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
            SELECT new com.personal.happygallery.application.review.port.out.ReviewReportListView(
                r.id, r.reviewId, r.reason, r.snapshotStatus, r.status, r.createdAt)
            FROM ReviewReport r
            WHERE (:status IS NULL OR r.status = :status)
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewReportListView> findAdminRows(
            @Param("status") ReviewReportStatus status, Pageable pageable);

    @Query("""
            SELECT new com.personal.happygallery.application.review.port.out.ReviewReportListView(
                r.id, r.reviewId, r.reason, r.snapshotStatus, r.status, r.createdAt)
            FROM ReviewReport r
            WHERE (:status IS NULL OR r.status = :status)
              AND (r.createdAt < :createdAt
                   OR (r.createdAt = :createdAt AND r.id < :id))
            ORDER BY r.createdAt DESC, r.id DESC
            """)
    List<ReviewReportListView> findAdminRowsAfter(
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

    @Query("""
            SELECT r FROM ReviewReport r
            WHERE r.status <> com.personal.happygallery.domain.review.ReviewReportStatus.PENDING
              AND r.decidedAt <= :cutoff
            ORDER BY r.decidedAt ASC, r.id ASC
            """)
    List<ReviewReport> findResolvedBefore(
            @Param("cutoff") LocalDateTime cutoff, Pageable pageable);
}
