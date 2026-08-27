package com.personal.happygallery.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_option_values")
public class ProductOptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "option_key", nullable = false, length = ProductOptionPolicy.MAX_KEY_LENGTH)
    private String key;

    @Column(nullable = false, length = ProductOptionPolicy.MAX_NAME_LENGTH)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ProductOptionValue() {}

    public ProductOptionValue(Long groupId, String key, String name, int sortOrder) {
        if (groupId == null) {
            throw new IllegalArgumentException("옵션 그룹은 필수입니다.");
        }
        this.groupId = groupId;
        this.key = ProductOptionPolicy.requireKey(key, "옵션값");
        update(name, sortOrder);
    }

    public void update(String name, int sortOrder) {
        this.name = ProductOptionPolicy.requireName(name, "옵션값");
        this.sortOrder = ProductOptionPolicy.requireSortOrder(sortOrder);
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() { return id; }
    public Long getGroupId() { return groupId; }
    public String getKey() { return key; }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }
    public boolean isActive() { return active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
