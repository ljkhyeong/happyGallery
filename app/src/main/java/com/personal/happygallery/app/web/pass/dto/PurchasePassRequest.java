package com.personal.happygallery.app.web.pass.dto;

import jakarta.validation.constraints.NotNull;

public record PurchasePassRequest(@NotNull Long guestId) {}
