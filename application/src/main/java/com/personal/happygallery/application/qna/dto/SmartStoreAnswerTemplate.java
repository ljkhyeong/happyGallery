package com.personal.happygallery.application.qna.dto;

/** 스마트스토어 상품 문의 답변 템플릿이다. */
public record SmartStoreAnswerTemplate(
        String questionType,
        String subject,
        String content
) {}
