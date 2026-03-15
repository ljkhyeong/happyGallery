package com.personal.happygallery.app.product;

import com.personal.happygallery.app.product.port.out.ProductReaderPort;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.infra.product.ProductRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * {@link ProductRepository}(infra) → {@link ProductReaderPort}(app) 브릿지 어댑터.
 */
@Component
class ProductReaderPortAdapter implements ProductReaderPort {

    private final ProductRepository productRepository;

    ProductReaderPortAdapter(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public Optional<Product> findById(Long id) {
        return productRepository.findById(id);
    }
}
