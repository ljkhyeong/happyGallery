package com.personal.happygallery.app.search;

import com.personal.happygallery.app.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.app.search.port.in.AdminOrderSearchUseCase;
import com.personal.happygallery.app.search.port.out.AdminOrderSearchPort;
import com.personal.happygallery.app.web.OffsetPage;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
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
        return AdminSearchHelper.search(searchPort, status, dateFrom, dateTo, keyword, page, size);
    }
}
