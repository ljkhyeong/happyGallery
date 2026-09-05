package com.personal.happygallery.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "member_favorites")
public class Favorite {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(name = "product_id") private Long productId;
    @Column(name = "class_id") private Long classId;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;

    protected Favorite() {}
    public Favorite(Long userId, FavoriteTargetType type, Long targetId, LocalDateTime createdAt) {
        this.userId = Objects.requireNonNull(userId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(targetId);
        if (type == FavoriteTargetType.PRODUCT) productId = targetId;
        else classId = targetId;
        this.createdAt = Objects.requireNonNull(createdAt);
    }
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getProductId() { return productId; }
    public Long getClassId() { return classId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
