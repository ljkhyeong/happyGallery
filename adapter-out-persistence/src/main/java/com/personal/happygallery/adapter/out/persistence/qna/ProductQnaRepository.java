package com.personal.happygallery.adapter.out.persistence.qna;

import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaListView;
import com.personal.happygallery.domain.qna.ProductQna;
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

public interface ProductQnaRepository extends JpaRepository<ProductQna, Long>, ProductQnaReaderPort {

    @Override Optional<ProductQna> findByIdAndProductId(Long id, Long productId);
    @Override Optional<ProductQna> findByIdAndProductIdAndUserId(Long id, Long productId, Long userId);
    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT q FROM ProductQna q WHERE q.id = :id")
    Optional<ProductQna> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            SELECT q.id AS id,
                   q.userId AS userId,
                   q.title AS title,
                   q.secret AS secret,
                   CASE WHEN q.replyContent IS NULL THEN false ELSE true END AS hasReply,
                   q.createdAt AS createdAt
            FROM ProductQna q
            WHERE q.productId = :productId
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQnaListProjection> findListByProductId(
            @Param("productId") Long productId, Pageable pageable);

    @Query("""
            SELECT q.id AS id,
                   q.userId AS userId,
                   q.title AS title,
                   q.secret AS secret,
                   CASE WHEN q.replyContent IS NULL THEN false ELSE true END AS hasReply,
                   q.createdAt AS createdAt
            FROM ProductQna q
            WHERE q.productId = :productId
              AND (q.createdAt < :createdAt
                   OR (q.createdAt = :createdAt AND q.id < :id))
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQnaListProjection> findListByProductIdAfter(
            @Param("productId") Long productId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

    @Query("""
            SELECT q.id AS id,
                   q.userId AS userId,
                   q.title AS title,
                   q.secret AS secret,
                   CASE WHEN q.replyContent IS NULL THEN false ELSE true END AS hasReply,
                   q.createdAt AS createdAt
            FROM ProductQna q
            WHERE q.productId = :productId
              AND q.userId = :userId
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQnaListProjection> findOwnedListByProductId(
            @Param("productId") Long productId,
            @Param("userId") Long userId,
            Pageable pageable);

    @Query("""
            SELECT q.id AS id,
                   q.userId AS userId,
                   q.title AS title,
                   q.secret AS secret,
                   CASE WHEN q.replyContent IS NULL THEN false ELSE true END AS hasReply,
                   q.createdAt AS createdAt
            FROM ProductQna q
            WHERE q.productId = :productId
              AND q.userId = :userId
              AND (q.createdAt < :createdAt
                   OR (q.createdAt = :createdAt AND q.id < :id))
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQnaListProjection> findOwnedListByProductIdAfter(
            @Param("productId") Long productId,
            @Param("userId") Long userId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

    List<ProductQna> findByProductIdOrderByCreatedAtDescIdDesc(
            Long productId, Pageable pageable);

    @Query("""
            SELECT q FROM ProductQna q
            WHERE q.productId = :productId
              AND (q.createdAt < :createdAt
                   OR (q.createdAt = :createdAt AND q.id < :id))
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQna> findByProductIdForAdminAfterPage(
            @Param("productId") Long productId,
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

    List<ProductQna> findByRepliedAtIsNullOrderByCreatedAtDescIdDesc(Pageable pageable);

    @Query("""
            SELECT q FROM ProductQna q
            WHERE q.repliedAt IS NULL
              AND (q.createdAt < :createdAt
                   OR (q.createdAt = :createdAt AND q.id < :id))
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQna> findUnansweredAfterPage(
            @Param("createdAt") LocalDateTime createdAt,
            @Param("id") Long id,
            Pageable pageable);

    @Override
    default List<ProductQnaListView> findByProductId(Long productId, int limit) {
        return toViews(findListByProductId(productId, PageRequest.ofSize(limit)));
    }

    @Override
    default List<ProductQnaListView> findByProductIdAfter(
            Long productId, LocalDateTime createdAt, Long id, int limit) {
        return toViews(findListByProductIdAfter(
                productId, createdAt, id, PageRequest.ofSize(limit)));
    }

    @Override
    default List<ProductQnaListView> findOwnedByProduct(
            Long productId, Long userId, int limit) {
        return toViews(findOwnedListByProductId(
                productId, userId, PageRequest.ofSize(limit)));
    }

    @Override
    default List<ProductQnaListView> findOwnedByProductAfter(
            Long productId, Long userId, LocalDateTime createdAt, Long id, int limit) {
        return toViews(findOwnedListByProductIdAfter(
                productId, userId, createdAt, id, PageRequest.ofSize(limit)));
    }

    @Override
    default List<ProductQna> findByProductIdForAdmin(Long productId, int limit) {
        return findByProductIdOrderByCreatedAtDescIdDesc(
                productId, PageRequest.ofSize(limit));
    }

    @Override
    default List<ProductQna> findByProductIdForAdminAfter(
            Long productId, LocalDateTime createdAt, Long id, int limit) {
        return findByProductIdForAdminAfterPage(
                productId, createdAt, id, PageRequest.ofSize(limit));
    }

    @Override
    default List<ProductQna> findUnanswered(int limit) {
        return findByRepliedAtIsNullOrderByCreatedAtDescIdDesc(PageRequest.ofSize(limit));
    }

    @Override
    default List<ProductQna> findUnansweredAfter(LocalDateTime createdAt, Long id, int limit) {
        return findUnansweredAfterPage(createdAt, id, PageRequest.ofSize(limit));
    }

    private static List<ProductQnaListView> toViews(
            List<ProductQnaListProjection> projections) {
        return projections.stream()
                .map(item -> new ProductQnaListView(
                        item.getId(), item.getUserId(), item.getTitle(), item.isSecret(),
                        item.getHasReply(), item.getCreatedAt()))
                .toList();
    }
}

interface ProductQnaListProjection {
    Long getId();
    Long getUserId();
    String getTitle();
    boolean isSecret();
    boolean getHasReply();
    LocalDateTime getCreatedAt();
}
