package com.personal.happygallery.app.web.customer.dto;

import jakarta.validation.constraints.Min;

public record UpdateCartItemRequest(
        @Min(1) int qty) {}
