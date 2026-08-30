package com.personal.happygallery.adapter.in.web.admin.dto;

import com.personal.happygallery.application.qna.port.in.SmartStoreInquiryUseCase.AnswerTemplateResult;
import io.swagger.v3.oas.annotations.media.Schema;

public record SmartStoreInquiryAnswerTemplateResponse(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String questionType,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String subject,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) String content
) {
    public static SmartStoreInquiryAnswerTemplateResponse from(AnswerTemplateResult result) {
        return new SmartStoreInquiryAnswerTemplateResponse(
                result.questionType(), result.subject(), result.content());
    }
}
