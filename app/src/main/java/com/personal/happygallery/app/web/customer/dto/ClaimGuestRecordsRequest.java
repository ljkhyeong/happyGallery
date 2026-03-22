package com.personal.happygallery.app.web.customer.dto;

import java.util.List;

public record ClaimGuestRecordsRequest(
        List<Long> orderIds,
        List<Long> bookingIds) {
    public ClaimGuestRecordsRequest {
        orderIds = orderIds == null ? null : List.copyOf(orderIds);
        bookingIds = bookingIds == null ? null : List.copyOf(bookingIds);
    }
}
