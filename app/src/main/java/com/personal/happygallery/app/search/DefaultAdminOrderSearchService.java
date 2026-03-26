package com.personal.happygallery.app.search;

import com.personal.happygallery.app.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.app.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.app.search.port.out.AdminOrderSearchPort;
import com.personal.happygallery.app.web.OffsetPage;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class DefaultAdminOrderSearchService implements AdminOrderSearchUseCase {

    private final AdminOrderSearchPort searchPort;

    DefaultAdminOrderSearchService(AdminOrderSearchPort searchPort) {
        this.searchPort = searchPort;
    }

    @Override
    public OffsetPage<AdminOrderSearchRow> search(OrderStatus status, LocalDate dateFrom, LocalDate dateTo,
                                                   String keyword, int page, int size) {
        long totalCount = searchPort.count(status, dateFrom, dateTo, keyword);
        if (totalCount == 0) {
            return OffsetPage.of(List.of(), page, size, 0);
        }
        List<AdminOrderSearchRow> rows = searchPort.search(status, dateFrom, dateTo, keyword, page * size, size);
        return OffsetPage.of(rows, page, size, totalCount);
    }
}
