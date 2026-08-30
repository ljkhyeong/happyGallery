package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.ShippingCarrier;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** POST /api/v1/admin/orders/{id}/mark-shipped 요청 바디. */
public record MarkShippedRequest(
        @NotBlank @Size(max = Fulfillment.MAX_CARRIER_LENGTH) String carrier,
        @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
        ShippingCarrier carrierCode,
        @NotBlank @Size(max = Fulfillment.MAX_TRACKING_NUMBER_LENGTH) String trackingNumber
) {}
