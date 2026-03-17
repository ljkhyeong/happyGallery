package com.personal.happygallery.infra.qna;

import com.personal.happygallery.domain.qna.ProductQna;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductQnaRepository extends JpaRepository<ProductQna, Long> {

    List<ProductQna> findByProductIdOrderByCreatedAtDesc(Long productId);
}
