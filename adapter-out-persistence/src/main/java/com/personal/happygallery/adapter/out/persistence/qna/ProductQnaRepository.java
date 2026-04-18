package com.personal.happygallery.adapter.out.persistence.qna;

import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.domain.qna.ProductQna;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductQnaRepository extends JpaRepository<ProductQna, Long>, ProductQnaReaderPort, ProductQnaStorePort {

    @Override Optional<ProductQna> findById(Long id);
    @Override ProductQna save(ProductQna qna);

    List<ProductQna> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Override
    default List<ProductQna> findByProductId(Long productId) {
        return findByProductIdOrderByCreatedAtDesc(productId);
    }
}
