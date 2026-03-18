package com.personal.happygallery.app.web.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record InquiryReplyRequest(@NotBlank String replyContent) {}
