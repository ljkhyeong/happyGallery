package com.personal.happygallery.application.qna.port.out;

import com.personal.happygallery.domain.qna.ProductQna;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductQnaReaderPort {

    Optional<ProductQna> findByIdForUpdate(Long id);

    Optional<ProductQna> findByIdAndProductId(Long id, Long productId);

    Optional<ProductQna> findByIdAndProductIdAndUserId(Long id, Long productId, Long userId);

    // 목록 조회는 커서 값이 null이면 첫 페이지를 조회한다. 정렬은 (createdAt DESC, id DESC)다.

    List<ProductQnaListView> findOwnedByProduct(
            Long productId, Long userId, LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    List<ProductQnaListView> findByProductId(
            Long productId, LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    List<ProductQna> findByProductIdForAdmin(
            Long productId, LocalDateTime cursorCreatedAt, Long cursorId, int limit);

    List<ProductQna> findUnanswered(LocalDateTime cursorCreatedAt, Long cursorId, int limit);
}
