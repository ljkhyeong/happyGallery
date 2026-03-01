package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** 판매 상품 — products 테이블 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ProductStatus status;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Product() {}

    /**
     * 상품 생성. 초기 상태는 {@link ProductStatus#ACTIVE}.
     *
     * @param name  상품명
     * @param type  상품 유형 (READY_STOCK | MADE_TO_ORDER)
     * @param price 가격 (원)
     */
    public Product(String name, ProductType type, long price) {
        this.name = name;
        this.type = type;
        this.price = price;
        this.status = ProductStatus.ACTIVE;
    }

    /** 상품을 비활성화한다. */
    public void deactivate() {
        this.status = ProductStatus.INACTIVE;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ProductType getType() { return type; }
    public long getPrice() { return price; }
    public ProductStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
