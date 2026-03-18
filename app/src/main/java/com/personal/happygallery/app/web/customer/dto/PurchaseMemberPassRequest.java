package com.personal.happygallery.app.web.customer.dto;

import jakarta.validation.constraints.Positive;

public record PurchaseMemberPassRequest(@Positive long totalPrice) {}
