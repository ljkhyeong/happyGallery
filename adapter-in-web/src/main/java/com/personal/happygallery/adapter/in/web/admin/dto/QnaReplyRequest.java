package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QnaReplyRequest(
        @NotBlank
        @Size(min = ContentTextPolicy.MIN_LENGTH, max = ContentTextPolicy.MAX_BODY_LENGTH)
        String replyContent
) {}
