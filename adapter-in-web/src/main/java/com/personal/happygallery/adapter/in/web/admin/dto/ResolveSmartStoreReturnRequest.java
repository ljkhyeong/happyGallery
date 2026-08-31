package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ResolveSmartStoreReturnRequest(@NotNull Boolean restoreStock, @NotBlank String reviewVersion) {}
