package com.personal.happygallery.adapter.out.persistence.qna;

import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.domain.qna.ProductQna;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

public interface ProductQnaRepository extends JpaRepository<ProductQna, Long>, ProductQnaReaderPort, ProductQnaStorePort {

    @Override Optional<ProductQna> findById(Long id);
    @Override Optional<ProductQna> findByIdAndProductId(Long id, Long productId);
    @Override ProductQna save(ProductQna qna);

    @Override
    @Lock(PESSIMISTIC_WRITE)
    @Query("SELECT q FROM ProductQna q WHERE q.id = :id")
    Optional<ProductQna> findByIdForUpdate(@Param("id") Long id);

    List<ProductQna> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Override
    default List<ProductQna> findByProductId(Long productId) {
        return findByProductIdOrderByCreatedAtDesc(productId);
    }
}
