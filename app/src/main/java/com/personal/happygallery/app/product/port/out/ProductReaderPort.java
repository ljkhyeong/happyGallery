package com.personal.happygallery.app.product.port.out;

import com.personal.happygallery.domain.product.Product;
import java.util.Optional;

public interface ProductReaderPort {
    Optional<Product> findById(Long id);
}
