package com.personal.happygallery.app.qna;

import com.personal.happygallery.app.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.app.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.infra.qna.ProductQnaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ProductQnaPersistencePortAdapter implements ProductQnaReaderPort, ProductQnaStorePort {

    private final ProductQnaRepository repository;

    ProductQnaPersistencePortAdapter(ProductQnaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<ProductQna> findById(Long id) {
        return repository.findById(id);
    }

    @Override
    public List<ProductQna> findByProductId(Long productId) {
        return repository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    @Override
    public ProductQna save(ProductQna qna) {
        return repository.save(qna);
    }
}
