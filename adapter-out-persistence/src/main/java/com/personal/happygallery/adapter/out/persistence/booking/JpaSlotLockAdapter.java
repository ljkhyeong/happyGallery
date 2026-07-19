package com.personal.happygallery.adapter.out.persistence.booking;

import com.personal.happygallery.application.booking.port.out.SlotLockPort;
import com.personal.happygallery.domain.booking.Slot;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class JpaSlotLockAdapter implements SlotLockPort {

    private final EntityManager entityManager;

    JpaSlotLockAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<Slot> lockAllById(List<Long> ids) {
        // 1차 캐시의 이전 값을 재사용하지 않도록 관련 Slot만 분리한 뒤 잠금 조회로 다시 적재한다.
        entityManager.flush();
        for (Long id : ids) {
            Slot slot = entityManager.find(Slot.class, id);
            if (slot != null) {
                entityManager.detach(slot);
            }
        }

        return lockByIds(ids);
    }

    @Override
    public List<Slot> lockScope(Long classId,
                               Long sourceSlotId,
                               LocalDateTime windowStart,
                               LocalDateTime windowEnd) {
        entityManager.flush();
        List<Slot> scope = entityManager.createQuery(
                        """
                        SELECT s
                        FROM Slot s
                        WHERE s.bookingClass.id = :classId
                          AND (s.id = :sourceSlotId
                               OR (s.startAt >= :windowStart AND s.startAt < :windowEnd))
                        ORDER BY s.id
                        """, Slot.class)
                .setParameter("classId", classId)
                .setParameter("sourceSlotId", sourceSlotId)
                .setParameter("windowStart", windowStart)
                .setParameter("windowEnd", windowEnd)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();

        // 범위 잠금은 신규 행까지 찾는 현재 읽기다. 해당 Slot만 분리해 이전 스냅샷 값을 버린 뒤 다시 적재한다.
        List<Long> ids = scope.stream().map(Slot::getId).toList();
        scope.forEach(entityManager::detach);
        return lockByIds(ids);
    }

    private List<Slot> lockByIds(List<Long> ids) {
        return entityManager.createQuery(
                        "SELECT s FROM Slot s WHERE s.id IN :ids ORDER BY s.id", Slot.class)
                .setParameter("ids", ids)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultList();
    }
}
