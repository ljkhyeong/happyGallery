package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.ProductOptionValuePort;
import com.personal.happygallery.domain.product.ProductOptionValue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionValueRepository
        extends JpaRepository<ProductOptionValue, Long>, ProductOptionValuePort {
}
