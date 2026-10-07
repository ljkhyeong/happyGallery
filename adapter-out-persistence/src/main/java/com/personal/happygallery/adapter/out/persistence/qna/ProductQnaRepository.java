package com.personal.happygallery.adapter.out.persistence.qna;

import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaListView;
import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
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

public interface ProductQnaRepository extends JpaRepository<ProductQna, Long>,
        ProductQnaReaderPort,
        ProductQnaStorePort {

    @Override
    <S extends ProductQna> S save(S qna);

    @Override Optional<ProductQna> findByIdAndProductId(Long id, Long productId);
    @Override Optional<ProductQna> findByIdAndProductIdAndUserId(Long id, Long productId, Long userId);
    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT q FROM ProductQna q WHERE q.id = :id")
    Optional<ProductQna> findByIdForUpdate(@Param("id") Long id);

    /** 공개·본인 Q&A 목록 공통 projection과 커서 조건. productId·커서 파라미터를 사용한다. */
    String LIST_VIEW_PAGE = """
            SELECT new com.personal.happygallery.application.qna.port.out.ProductQnaListView(
                q.id, q.userId, q.title, q.secret,
                CASE WHEN q.replyContent IS NULL THEN false ELSE true END,
                q.createdAt
            )
            FROM ProductQna q
            WHERE q.productId = :productId
              AND (:cursorCreatedAt IS NULL
                   OR q.createdAt < :cursorCreatedAt
                   OR (q.createdAt = :cursorCreatedAt AND q.id < :cursorId))
            """;

    @Query(LIST_VIEW_PAGE + """
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQnaListView> findListPage(
            @Param("productId") Long productId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query(LIST_VIEW_PAGE + """
              AND q.userId = :userId
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQnaListView> findOwnedListPage(
            @Param("productId") Long productId,
            @Param("userId") Long userId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("""
            SELECT q FROM ProductQna q
            WHERE q.productId = :productId
              AND (:cursorCreatedAt IS NULL
                   OR q.createdAt < :cursorCreatedAt
                   OR (q.createdAt = :cursorCreatedAt AND q.id < :cursorId))
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQna> findAdminPageByProductId(
            @Param("productId") Long productId,
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Query("""
            SELECT q FROM ProductQna q
            WHERE q.repliedAt IS NULL
              AND (:cursorCreatedAt IS NULL
                   OR q.createdAt < :cursorCreatedAt
                   OR (q.createdAt = :cursorCreatedAt AND q.id < :cursorId))
            ORDER BY q.createdAt DESC, q.id DESC
            """)
    List<ProductQna> findUnansweredPage(
            @Param("cursorCreatedAt") LocalDateTime cursorCreatedAt,
            @Param("cursorId") Long cursorId,
            Pageable pageable);

    @Override
    default List<ProductQnaListView> findByProductId(
            Long productId, LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findListPage(productId, cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }

    @Override
    default List<ProductQnaListView> findOwnedByProduct(
            Long productId, Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findOwnedListPage(
                productId, userId, cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }

    @Override
    default List<ProductQna> findByProductIdForAdmin(
            Long productId, LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findAdminPageByProductId(productId, cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }

    @Override
    default List<ProductQna> findUnanswered(LocalDateTime cursorCreatedAt, Long cursorId, int limit) {
        return findUnansweredPage(cursorCreatedAt, cursorId, PageRequest.ofSize(limit));
    }
}
