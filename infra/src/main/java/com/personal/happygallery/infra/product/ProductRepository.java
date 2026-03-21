package com.personal.happygallery.infra.product;

import com.personal.happygallery.app.product.port.out.ProductReaderPort;
import com.personal.happygallery.app.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductReaderPort, ProductStorePort {

    @Override Optional<Product> findById(Long id);
    @Override Product save(Product product);

    /** ACTIVE 상품 목록 — 최신 등록순 */
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);
}
