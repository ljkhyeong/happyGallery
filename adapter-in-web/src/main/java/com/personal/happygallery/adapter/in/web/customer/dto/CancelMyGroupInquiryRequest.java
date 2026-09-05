package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CancelMyGroupInquiryRequest(@NotNull @PositiveOrZero Long version) {}
