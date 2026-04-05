package com.personal.happygallery.app.search.port.out;

import com.personal.happygallery.app.search.dto.AdminOrderSearchRow;
import com.personal.happygallery.domain.order.OrderStatus;
import java.time.LocalDate;
import java.util.List;

public interface AdminOrderSearchPort extends AdminSearchPort<OrderStatus, AdminOrderSearchRow> {
}
