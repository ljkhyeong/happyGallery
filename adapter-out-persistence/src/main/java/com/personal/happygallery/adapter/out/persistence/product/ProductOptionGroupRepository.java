package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.port.out.ProductOptionGroupPort;
import com.personal.happygallery.domain.product.ProductOptionGroup;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductOptionGroupRepository
        extends JpaRepository<ProductOptionGroup, Long>, ProductOptionGroupPort {
}
