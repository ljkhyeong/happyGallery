package com.personal.happygallery.application.qna.port.out;

import com.personal.happygallery.domain.qna.ProductQna;
import java.util.List;
import java.util.Optional;

public interface ProductQnaReaderPort {

    Optional<ProductQna> findById(Long id);

    List<ProductQna> findByProductId(Long productId);
}
