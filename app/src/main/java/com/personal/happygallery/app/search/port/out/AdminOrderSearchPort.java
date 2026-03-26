package com.personal.happygallery.app.search.port.out;

import com.personal.happygallery.app.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;

public interface AdminOrderSearchPort {

    List<AdminOrderSearchRow> search(OrderStatus status, LocalDate dateFrom, LocalDate dateTo,
                                      String keyword, int offset, int size);

    long count(OrderStatus status, LocalDate dateFrom, LocalDate dateTo, String keyword);
}
