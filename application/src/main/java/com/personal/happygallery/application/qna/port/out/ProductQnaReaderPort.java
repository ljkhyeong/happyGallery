package com.personal.happygallery.application.qna.port.out;

import com.personal.happygallery.domain.qna.ProductQna;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProductQnaReaderPort {

    Optional<ProductQna> findById(Long id);

    Optional<ProductQna> findByIdForUpdate(Long id);

    Optional<ProductQna> findByIdAndProductId(Long id, Long productId);

    List<ProductQna> findByProductId(Long productId);

    List<ProductQna> findUnanswered(int limit);

    List<ProductQna> findUnansweredAfter(LocalDateTime createdAt, Long id, int limit);
}
