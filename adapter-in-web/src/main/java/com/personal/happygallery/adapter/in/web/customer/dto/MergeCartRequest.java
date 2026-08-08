package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record MergeCartRequest(
        @NotNull UUID idempotencyKey,
        @NotNull @Size(min = 1, max = 100) List<@NotNull @Valid MergeCartItemRequest> items) {}
