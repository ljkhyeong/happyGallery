package com.personal.happygallery.adapter.out.persistence.dashboard.adapter;

import com.personal.happygallery.adapter.out.persistence.dashboard.mapper.AdminOrderSearchMapper;
import com.personal.happygallery.adapter.out.persistence.time.SeoulDateTimeRangeConverter;
import com.personal.happygallery.application.search.port.out.AdminOrderSearchPort;
import com.personal.happygallery.application.search.port.out.AdminOrderSearchResult;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** DB UTC 기본값으로 저장된 주문 생성 시각의 KST 날짜 범위를 MyBatis 매퍼에 전달한다. */
@Component
class MyBatisAdminOrderSearchAdapter implements AdminOrderSearchPort {

    private static final Pattern FORMATTED_ORDER_ID = Pattern.compile(
            "^ORD-(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final AdminOrderSearchMapper mapper;
    private final BlindIndexer blindIndexer;

    MyBatisAdminOrderSearchAdapter(AdminOrderSearchMapper mapper, BlindIndexer blindIndexer) {
        this.mapper = mapper;
        this.blindIndexer = blindIndexer;
    }

    @Override
    public List<AdminOrderSearchResult> search(OrderStatus status, LocalDate dateFrom, LocalDate dateTo,
                                                String keyword, int offset, int size) {
        AdminSearchKeyword searchKeyword = AdminSearchKeyword.parse(keyword, FORMATTED_ORDER_ID);
        AdminSearchIndexes indexes =
                AdminSearchIndexes.from(searchKeyword.keyword(), blindIndexer);
        return mapper.search(
                status != null ? status.name() : null,
                dateFrom != null ? SeoulDateTimeRangeConverter.toUtcStart(dateFrom) : null,
                dateTo != null ? SeoulDateTimeRangeConverter.toUtcExclusiveEnd(dateTo) : null,
                searchKeyword.keyword(), indexes.nameHmac(), indexes.phoneHmac(),
                searchKeyword.exactId(), offset, size);
    }

    @Override
    public long count(OrderStatus status, LocalDate dateFrom, LocalDate dateTo, String keyword) {
        AdminSearchKeyword searchKeyword = AdminSearchKeyword.parse(keyword, FORMATTED_ORDER_ID);
        AdminSearchIndexes indexes =
                AdminSearchIndexes.from(searchKeyword.keyword(), blindIndexer);
        return mapper.count(
                status != null ? status.name() : null,
                dateFrom != null ? SeoulDateTimeRangeConverter.toUtcStart(dateFrom) : null,
                dateTo != null ? SeoulDateTimeRangeConverter.toUtcExclusiveEnd(dateTo) : null,
                searchKeyword.keyword(), indexes.nameHmac(), indexes.phoneHmac(),
                searchKeyword.exactId());
    }
}
