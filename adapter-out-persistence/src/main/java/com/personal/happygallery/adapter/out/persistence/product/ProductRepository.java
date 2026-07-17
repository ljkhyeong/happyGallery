package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.ProductFilter;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.product.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product>, ProductReaderPort, ProductStorePort {

    @Override Optional<Product> findById(Long id);

    @Override
    default List<Product> findAllById(List<Long> ids) {
        return findAllById((Iterable<Long>) ids);
    }

    @Override Product save(Product product);

    /** ACTIVE 상품 목록 — 최신 등록순 */
    @Override
    @Query("""
            SELECT p FROM Product p
            WHERE p.status = com.personal.happygallery.domain.product.ProductStatus.ACTIVE
            ORDER BY p.createdAt DESC
            """)
    List<Product> findActiveProductsByCreatedAtDesc();

    /** ACTIVE 상품의 카테고리 목록 (distinct, non-null). */
    @Override
    @Query("""
            SELECT DISTINCT p.category FROM Product p
            WHERE p.status = com.personal.happygallery.domain.product.ProductStatus.ACTIVE
              AND p.category IS NOT NULL
            ORDER BY p.category
            """)
    List<String> findDistinctActiveCategories();

    /** 필터 조건에 따른 ACTIVE 상품 목록 조회. */
    @Override
    default List<Product> findActiveByFilter(ProductFilter filter) {
        Specification<Product> spec = andIfPresent(
                ProductSpecifications.isActive(),
                ProductSpecifications.hasType(filter.type()));
        spec = andIfPresent(spec, ProductSpecifications.hasCategory(filter.category()));
        spec = andIfPresent(spec, ProductSpecifications.nameContains(filter.keyword()));

        Sort sort = switch (filter.sort()) {
            case PRICE_ASC -> Sort.by("price").ascending();
            case PRICE_DESC -> Sort.by("price").descending();
            case NEWEST -> Sort.by("createdAt").descending();
        };

        return findAll(spec, sort);
    }

    private static Specification<Product> andIfPresent(Specification<Product> spec,
                                                       Specification<Product> optionalSpec) {
        return optionalSpec == null ? spec : spec.and(optionalSpec);
    }
}
