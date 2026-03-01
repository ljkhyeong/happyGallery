package com.personal.happygallery.infra.product;

import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {

    /** ACTIVE 상품 목록 — 최신 등록순 */
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);
}
