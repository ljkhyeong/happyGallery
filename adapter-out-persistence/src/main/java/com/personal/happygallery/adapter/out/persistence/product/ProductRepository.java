package com.personal.happygallery.adapter.out.persistence.product;

import com.personal.happygallery.application.product.ProductFilter;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.product.port.out.ProductStorePort;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.data.repository.query.Param;
import org.springframework.util.StringUtils;

public interface ProductRepository extends JpaRepository<Product, Long>,
        ProductReaderPort,
        ProductStorePort {

    @Override
    <S extends Product> S save(S product);

    @Override Optional<Product> findById(Long id);

    Optional<Product> findByIdAndStatus(Long id, ProductStatus status);

    @Override
    default Optional<Product> findActiveById(Long id) {
        return findByIdAndStatus(id, ProductStatus.ACTIVE);
    }

    /** ACTIVE 상품 목록 — 최신 등록순 */
    List<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status);

    @Override
    default List<Product> findActiveProductsByCreatedAtDesc() {
        return findByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE);
    }

    List<Product> findAllByOrderByCreatedAtDesc();

    @Override
    default List<Product> findAllProductsByCreatedAtDesc() {
        return findAllByOrderByCreatedAtDesc();
    }

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
        Sort sort = switch (filter.sort()) {
            case PRICE_ASC -> Sort.by("price").ascending();
            case PRICE_DESC -> Sort.by("price").descending();
            case NEWEST -> Sort.by("createdAt").descending();
        };

        return findActiveByConditions(
                filter.type(),
                filter.category(),
                toLikePattern(filter.keyword()),
                sort);
    }

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = com.personal.happygallery.domain.product.ProductStatus.ACTIVE
              AND (:type IS NULL OR p.type = :type)
              AND (:category IS NULL OR p.category = :category)
              AND (:keywordPattern IS NULL OR p.name LIKE :keywordPattern ESCAPE '!')
            """)
    List<Product> findActiveByConditions(
            @Param("type") ProductType type,
            @Param("category") String category,
            @Param("keywordPattern") String keywordPattern,
            Sort sort);

    private static String toLikePattern(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }

        String escaped = EscapeCharacter.of('!').escape(keyword);
        return "%" + escaped + "%";
    }
}
