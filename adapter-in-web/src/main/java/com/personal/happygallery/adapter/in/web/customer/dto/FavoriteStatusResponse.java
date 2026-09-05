package com.personal.happygallery.adapter.in.web.customer.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FavoriteStatusResponse(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean saved) {}
