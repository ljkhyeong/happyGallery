package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.Size;

public record FailNextRefundRequest(@Size(max = 120) String reason) {}
