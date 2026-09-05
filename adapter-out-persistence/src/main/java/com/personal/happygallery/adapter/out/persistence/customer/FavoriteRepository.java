package com.personal.happygallery.adapter.out.persistence.customer;

import com.personal.happygallery.application.customer.port.in.FavoriteUseCase.View;
import com.personal.happygallery.application.customer.port.out.FavoritePort;
import com.personal.happygallery.domain.user.Favorite;
import com.personal.happygallery.domain.user.FavoriteTargetType;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class FavoriteRepository implements FavoritePort {
    private final EntityManager em;
    FavoriteRepository(EntityManager em) { this.em = em; }
    @Override
    public Favorite save(Favorite favorite) { em.persist(favorite); return favorite; }
    @Override
    public boolean exists(Long userId, FavoriteTargetType type, Long targetId) {
        return em.createQuery("SELECT count(f) FROM Favorite f WHERE f.userId = :userId AND " + targetPath(type) + " = :targetId", Long.class)
                .setParameter("userId", userId).setParameter("targetId", targetId).getSingleResult() > 0;
    }
    @Override
    public void deleteTarget(Long userId, FavoriteTargetType type, Long targetId) {
        em.createQuery("DELETE FROM Favorite f WHERE f.userId = :userId AND " + targetPath(type) + " = :targetId")
                .setParameter("userId", userId).setParameter("targetId", targetId).executeUpdate();
    }
    @Override
    public void deleteByUserId(Long userId) {
        em.createQuery("DELETE FROM Favorite f WHERE f.userId = :userId").setParameter("userId", userId).executeUpdate();
    }
    @Override
    public List<View> list(Long userId, FavoriteTargetType type, LocalDateTime before, Long beforeId, int limit) {
        String typeClause = type == null ? "" : " AND " + targetPath(type) + " IS NOT NULL";
        return em.createQuery("""
                SELECT new com.personal.happygallery.application.customer.port.in.FavoriteUseCase$View(
                    f.id,
                    CASE WHEN f.productId IS NOT NULL THEN com.personal.happygallery.domain.user.FavoriteTargetType.PRODUCT
                         ELSE com.personal.happygallery.domain.user.FavoriteTargetType.CLASS END,
                    coalesce(f.productId, f.classId), coalesce(p.name, c.name),
                    CASE WHEN p.status = com.personal.happygallery.domain.product.ProductStatus.ACTIVE
                           OR c.status = com.personal.happygallery.domain.booking.BookingClassStatus.ACTIVE THEN true ELSE false END,
                    f.createdAt)
                FROM Favorite f LEFT JOIN Product p ON p.id = f.productId LEFT JOIN BookingClass c ON c.id = f.classId
                WHERE f.userId = :userId
                  AND (:before IS NULL OR f.createdAt < :before OR (f.createdAt = :before AND f.id < :beforeId))
                """ + typeClause + " ORDER BY f.createdAt DESC, f.id DESC", View.class)
                .setParameter("userId", userId).setParameter("before", before).setParameter("beforeId", beforeId)
                .setMaxResults(limit).getResultList();
    }
    private static String targetPath(FavoriteTargetType type) {
        return switch (type) { case PRODUCT -> "f.productId"; case CLASS -> "f.classId"; };
    }
}
