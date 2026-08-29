package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record SynchronizeSmartStoreSettlementRequest(
        @NotNull LocalDate from,
        @NotNull LocalDate to
) {}
