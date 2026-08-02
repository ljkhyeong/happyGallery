package com.personal.happygallery.adapter.out.persistence.qna;

import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.domain.qna.ProductQna;
import org.springframework.stereotype.Repository;

@Repository
class JpaProductQnaPersistenceAdapter implements ProductQnaStorePort {

    private final ProductQnaRepository repository;

    JpaProductQnaPersistenceAdapter(ProductQnaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ProductQna save(ProductQna qna) {
        return repository.save(qna);
    }
}
