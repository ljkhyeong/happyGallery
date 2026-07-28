package com.personal.happygallery.adapter.out.persistence.qna;

import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
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

public interface ProductQnaRepository extends JpaRepository<ProductQna, Long>, ProductQnaReaderPort, ProductQnaStorePort {

    @Override Optional<ProductQna> findByIdAndProductId(Long id, Long productId);
    @Override Optional<ProductQna> findByIdAndProductIdAndUserId(Long id, Long productId, Long userId);
    List<ProductQna> findByProductIdAndUserIdOrderByCreatedAtDesc(Long productId, Long userId);
    @Override ProductQna save(ProductQna qna);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT q FROM ProductQna q WHERE q.id = :id")
    Optional<ProductQna> findByIdForUpdate(@Param("id") Long id);

    List<ProductQna> findByProductIdOrderByCreatedAtDesc(Long productId);

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
    default List<ProductQna> findByProductId(Long productId) {
        return findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    default List<ProductQna> findOwnedByProduct(Long productId, Long userId) {
        return findByProductIdAndUserIdOrderByCreatedAtDesc(productId, userId);
    }

    @Override
    default List<ProductQna> findUnanswered(int limit) {
        return findByRepliedAtIsNullOrderByCreatedAtDescIdDesc(PageRequest.ofSize(limit));
    }

    @Override
    default List<ProductQna> findUnansweredAfter(LocalDateTime createdAt, Long id, int limit) {
        return findUnansweredAfterPage(createdAt, id, PageRequest.ofSize(limit));
    }
}
