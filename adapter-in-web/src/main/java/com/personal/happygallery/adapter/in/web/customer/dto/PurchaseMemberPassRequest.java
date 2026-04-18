package com.personal.happygallery.adapter.in.web.customer.dto;

import jakarta.validation.constraints.Positive;

public record PurchaseMemberPassRequest(@Positive long totalPrice) {}
