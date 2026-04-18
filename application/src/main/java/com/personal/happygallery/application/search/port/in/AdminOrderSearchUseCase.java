package com.personal.happygallery.application.search.port.in;

import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.application.shared.page.OffsetPage;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;

public interface AdminOrderSearchUseCase {

    OffsetPage<AdminOrderSearchRow> search(OrderStatus status, LocalDate dateFrom, LocalDate dateTo,
                                            String keyword, int page, int size);
}
