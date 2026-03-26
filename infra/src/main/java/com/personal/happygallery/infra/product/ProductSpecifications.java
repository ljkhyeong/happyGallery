package com.personal.happygallery.infra.product;

import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductStatus;
import com.personal.happygallery.domain.product.ProductType;
import org.springframework.data.jpa.domain.Specification;

/**
 * Product 목록 조회용 JPA Specification 유틸.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {}

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.equal(root.get("status"), ProductStatus.ACTIVE);
    }

    public static Specification<Product> hasType(ProductType type) {
        if (type == null) return null;
        return (root, query, cb) -> cb.equal(root.get("type"), type);
    }

    public static Specification<Product> hasCategory(String category) {
        if (category == null) return null;
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    public static Specification<Product> nameContains(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return (root, query, cb) -> {
            String escaped = keyword.toLowerCase()
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            return cb.like(cb.lower(root.get("name")), "%" + escaped + "%", '\\');
        };
    }
}
