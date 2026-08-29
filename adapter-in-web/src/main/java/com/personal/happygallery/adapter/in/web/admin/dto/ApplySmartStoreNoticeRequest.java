package com.personal.happygallery.adapter.in.web.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ApplySmartStoreNoticeRequest(
        @NotEmpty List<@NotNull Long> channelProductNos
) {}
