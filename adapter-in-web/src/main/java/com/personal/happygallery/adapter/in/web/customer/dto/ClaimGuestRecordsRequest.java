package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ClaimGuestRecordsRequest(
        @Size(max = 100) List<@NotNull @Positive Long> orderIds,
        @Size(max = 100) List<@NotNull @Positive Long> bookingIds) {
    public ClaimGuestRecordsRequest {
        orderIds = orderIds == null ? List.of() : List.copyOf(orderIds);
        bookingIds = bookingIds == null ? List.of() : List.copyOf(bookingIds);
    }
}
