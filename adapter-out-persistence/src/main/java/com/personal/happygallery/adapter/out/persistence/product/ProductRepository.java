package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.ProductFilter;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product>, ProductReaderPort, ProductStorePort {

    @Override Optional<Product> findById(Long id);
    @Override Product save(Product product);

    /** ACTIVE 상품 목록 — 최신 등록순 */
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);

    /** ACTIVE 상품의 카테고리 목록 (distinct, non-null). */
    @Override
    @Query("SELECT DISTINCT p.category FROM Product p WHERE p.status = :status AND p.category IS NOT NULL ORDER BY p.category")
    List<String> findDistinctCategoriesByStatus(@Param("status") ProductStatus status);

    /** 필터 조건에 따른 ACTIVE 상품 목록 조회. */
    @Override
    default List<Product> findActiveByFilter(ProductFilter filter) {
        Specification<Product> spec = Specification.where(ProductSpecifications.isActive())
                .and(ProductSpecifications.hasType(filter.type()))
                .and(ProductSpecifications.hasCategory(filter.category()))
                .and(ProductSpecifications.nameContains(filter.keyword()));

        Sort sort = switch (filter.sort()) {
            case PRICE_ASC -> Sort.by("price").ascending();
            case PRICE_DESC -> Sort.by("price").descending();
            case NEWEST -> Sort.by("createdAt").descending();
        };

        return findAll(spec, sort);
    }
}
