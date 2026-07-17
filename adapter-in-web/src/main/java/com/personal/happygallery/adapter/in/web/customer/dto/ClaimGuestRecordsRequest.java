package com.personal.happygallery.adapter.in.web.customer.dto;

import java.util.List;

public record ClaimGuestRecordsRequest(
        List<Long> orderIds,
        List<Long> bookingIds) {
    public ClaimGuestRecordsRequest {
        orderIds = orderIds == null ? List.of() : List.copyOf(orderIds);
        bookingIds = bookingIds == null ? List.of() : List.copyOf(bookingIds);
    }
}
