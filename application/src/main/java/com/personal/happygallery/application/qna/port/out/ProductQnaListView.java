package com.personal.happygallery.application.qna.port.out;

import java.time.LocalDateTime;

/** 공개·작성자 Q&A 목록에 필요한 경량 조회 모델. */
public record ProductQnaListView(
        Long id,
        Long userId,
        String title,
        boolean secret,
        boolean hasReply,
        LocalDateTime createdAt
) {}
