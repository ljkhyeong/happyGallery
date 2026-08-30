package com.personal.happygallery.application.qna.port.out;

import com.personal.happygallery.domain.qna.ProductQna;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductQnaReaderPort {

    Optional<ProductQna> findByIdForUpdate(Long id);

    Optional<ProductQna> findByIdAndProductId(Long id, Long productId);

    Optional<ProductQna> findByIdAndProductIdAndUserId(Long id, Long productId, Long userId);

    List<ProductQnaListView> findOwnedByProduct(Long productId, Long userId, int limit);

    List<ProductQnaListView> findOwnedByProductAfter(
            Long productId, Long userId, LocalDateTime createdAt, Long id, int limit);

    List<ProductQnaListView> findByProductId(Long productId, int limit);

    List<ProductQnaListView> findByProductIdAfter(
            Long productId, LocalDateTime createdAt, Long id, int limit);

    List<ProductQna> findByProductIdForAdmin(Long productId, int limit);

    List<ProductQna> findByProductIdForAdminAfter(
            Long productId, LocalDateTime createdAt, Long id, int limit);

    List<ProductQna> findUnanswered(int limit);

    List<ProductQna> findUnansweredAfter(LocalDateTime createdAt, Long id, int limit);
}
