package com.personal.happygallery.app.product;

import com.personal.happygallery.app.product.port.out.ProductReaderPort;
import com.personal.happygallery.app.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.infra.product.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link ProductRepository}(infra) → {@link ProductReaderPort} + {@link ProductStorePort}(app) 브릿지 어댑터.
 */
@Component
class ProductPersistencePortAdapter implements ProductReaderPort, ProductStorePort {

    private final ProductRepository productRepository;

    ProductPersistencePortAdapter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }

    @Override
    public List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status) {
        return productRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Override
    public Product save(Product product) {
        return productRepository.save(product);
    }
}
