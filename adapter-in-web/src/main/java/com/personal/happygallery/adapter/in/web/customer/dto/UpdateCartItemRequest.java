package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(1) int qty) {}
