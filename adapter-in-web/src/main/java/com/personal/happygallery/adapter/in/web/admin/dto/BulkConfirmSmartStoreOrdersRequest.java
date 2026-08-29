package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkConfirmSmartStoreOrdersRequest(
        @NotNull @Size(min = 1, max = 30) List<@NotBlank String> productOrderIds
) {}
