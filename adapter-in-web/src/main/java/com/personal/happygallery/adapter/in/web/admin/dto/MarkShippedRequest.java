package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.order.Fulfillment;
import com.personal.happygallery.domain.order.ShippingCarrier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** POST /api/v1/admin/orders/{id}/mark-shipped 요청 바디. 택배사 표시명은 코드에서 파생한다. */
public record MarkShippedRequest(
        @NotNull ShippingCarrier carrierCode,
        @NotBlank @Size(max = Fulfillment.MAX_TRACKING_NUMBER_LENGTH) String trackingNumber
) {}
