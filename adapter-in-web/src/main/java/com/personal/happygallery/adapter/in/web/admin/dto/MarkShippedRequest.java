package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.order.Fulfillment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/v1/admin/orders/{id}/mark-shipped 요청 바디. */
public record MarkShippedRequest(
        @NotBlank @Size(max = Fulfillment.MAX_CARRIER_LENGTH) String carrier,
        @NotBlank @Size(max = Fulfillment.MAX_TRACKING_NUMBER_LENGTH) String trackingNumber
) {}
