package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.booking.BookingClassStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateClassStatusRequest(@NotNull BookingClassStatus status) {}
