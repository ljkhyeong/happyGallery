package com.personal.happygallery.adapter.out.persistence.customer;

import com.personal.happygallery.application.booking.BookingHistoryQuery;
import com.personal.happygallery.application.customer.port.out.MemberHistoryReaderPort;
import com.personal.happygallery.application.order.OrderHistoryQuery;
import com.personal.happygallery.application.pass.PassHistoryQuery;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.MemberHistoryCursor;
import com.personal.happygallery.domain.booking.Booking;
import com.personal.happygallery.domain.order.Order;
import com.personal.happygallery.domain.pass.PassPurchase;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import org.springframework.data.jpa.repository.query.EscapeCharacter;
import org.springframework.stereotype.Repository;

/** 검색 조건을 먼저 적용하고 정렬값·거래 번호 기준으로 다음 페이지를 조회한다. */
@Repository
class MemberHistoryRepository implements MemberHistoryReaderPort {
    private final EntityManager entityManager;

    MemberHistoryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public CursorPage<Order> findOrders(Long userId, OrderHistoryQuery query, String cursor, int size) {
        SortKey key = switch (query.sort()) {
            case LATEST -> new SortKey("o.createdAt", false, false);
            case OLDEST -> new SortKey("o.createdAt", true, false);
            case AMOUNT_DESC -> new SortKey("o.totalAmount", false, true);
            case AMOUNT_ASC -> new SortKey("o.totalAmount", true, true);
        };
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        StringBuilder where = new StringBuilder("o.userId = :userId");
        if (query.status() != null) {
            where.append(" AND o.status = :status");
            parameters.put("status", query.status());
        }
        if (query.keyword() != null) {
            where.append(" AND cast(o.id as String) LIKE :keyword ESCAPE '!'");
            parameters.put("keyword", keywordPattern(query.keyword()));
        }
        return page(Order.class, "SELECT o FROM Order o", "o.id", where, parameters, key,
                scope(userId, "orders", query.sort(), query.status(), query.keyword()), cursor, size,
                order -> key.numeric() ? order.getTotalAmount() : order.getCreatedAt(), Order::getId);
    }

    @Override
    public CursorPage<Booking> findBookings(Long userId, BookingHistoryQuery query, String cursor, int size) {
        SortKey key = switch (query.sort()) {
            case CREATED_DESC -> new SortKey("b.createdAt", false, false);
            case SOONEST -> new SortKey("b.slot.startAt", true, false);
            case LATEST -> new SortKey("b.slot.startAt", false, false);
            case DEPOSIT_DESC -> new SortKey("b.depositAmount", false, true);
        };
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        StringBuilder where = new StringBuilder("b.userId = :userId");
        if (query.status() != null) {
            where.append(" AND b.status = :status");
            parameters.put("status", query.status());
        }
        if (query.keyword() != null) {
            where.append(" AND (cast(b.id as String) LIKE :keyword ESCAPE '!'"
                    + " OR lower(b.bookingClass.name) LIKE lower(:keyword) ESCAPE '!')");
            parameters.put("keyword", keywordPattern(query.keyword()));
        }
        return page(Booking.class, "SELECT b FROM Booking b JOIN FETCH b.bookingClass JOIN FETCH b.slot",
                "b.id", where, parameters, key,
                scope(userId, "bookings", query.sort(), query.status(), query.keyword()), cursor, size,
                booking -> switch (query.sort()) {
                    case CREATED_DESC -> booking.getCreatedAt();
                    case SOONEST, LATEST -> booking.getSlot().getStartAt();
                    case DEPOSIT_DESC -> booking.getDepositAmount();
                }, Booking::getId);
    }

    @Override
    public CursorPage<PassPurchase> findPasses(Long userId, PassHistoryQuery query, String cursor, int size,
                                              LocalDateTime now) {
        SortKey key = switch (query.sort()) {
            case PURCHASE_DESC -> new SortKey("p.purchasedAt", false, false);
            case EXPIRY_ASC -> new SortKey("p.expiresAt", true, false);
            case CREDITS_DESC -> new SortKey("cast(p.remainingCredits as Long)", false, true);
        };
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("userId", userId);
        StringBuilder where = new StringBuilder("p.userId = :userId");
        if (query.status() != null) {
            switch (query.status()) {
                case USED_UP -> where.append(" AND p.remainingCredits <= 0");
                case ACTIVE -> where.append(" AND p.remainingCredits > 0 AND p.expiresAt > :now");
                case EXPIRED -> where.append(" AND p.remainingCredits > 0 AND p.expiresAt <= :now");
            }
            if (query.status() != PassHistoryQuery.PassHistoryStatus.USED_UP) parameters.put("now", now);
        }
        if (query.keyword() != null) {
            where.append(" AND cast(p.id as String) LIKE :keyword ESCAPE '!'");
            parameters.put("keyword", keywordPattern(query.keyword()));
        }
        return page(PassPurchase.class, "SELECT p FROM PassPurchase p", "p.id", where, parameters, key,
                scope(userId, "passes", query.sort(), query.status(), query.keyword()), cursor, size,
                pass -> switch (query.sort()) {
                    case PURCHASE_DESC -> pass.getPurchasedAt();
                    case EXPIRY_ASC -> pass.getExpiresAt();
                    case CREDITS_DESC -> pass.getRemainingCredits();
                }, PassPurchase::getId);
    }

    private <T> CursorPage<T> page(Class<T> type, String select, String idPath, StringBuilder where,
                                   Map<String, Object> parameters, SortKey key, String scope,
                                   String cursor, int size, Function<T, Object> value, Function<T, Long> id) {
        String comparison = key.ascending() ? ">" : "<";
        String direction = key.ascending() ? " ASC" : " DESC";
        if (cursor != null) {
            var decoded = MemberHistoryCursor.decode(cursor, scope);
            where.append(" AND (").append(key.path()).append(' ').append(comparison)
                    .append(" :cursorValue OR (").append(key.path()).append(" = :cursorValue AND ")
                    .append(idPath).append(' ').append(comparison).append(" :cursorId))");
            parameters.put("cursorValue", decoded.sortValue(key.numeric()));
            parameters.put("cursorId", decoded.id());
        }
        // 정렬 경로와 방향은 위 enum 분기만 사용하고 입력값은 모두 바인딩한다.
        var statement = entityManager.createQuery(select + " WHERE " + where + " ORDER BY "
                + key.path() + direction + ", " + idPath + direction, type);
        parameters.forEach(statement::setParameter);
        var rows = statement.setMaxResults(size + 1).getResultList();
        return CursorPage.of(rows, size, row -> MemberHistoryCursor.encode(scope, value.apply(row), id.apply(row)));
    }

    private static String scope(Long userId, String kind, Enum<?> sort, Enum<?> status, String keyword) {
        return userId + "|" + kind + "|" + sort + "|" + status + "|" + Objects.toString(keyword, "");
    }

    private static String keywordPattern(String keyword) {
        return "%" + EscapeCharacter.of('!').escape(keyword) + "%";
    }

    private record SortKey(String path, boolean ascending, boolean numeric) {}
}
