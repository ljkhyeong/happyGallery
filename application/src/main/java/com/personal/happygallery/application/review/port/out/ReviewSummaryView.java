package com.personal.happygallery.application.review.port.out;

/** 공개 후기 전체 집계와 1~5점 분포 조회 모델. */
public record ReviewSummaryView(
        long reviewCount,
        double averageRating,
        long rating1,
        long rating2,
        long rating3,
        long rating4,
        long rating5
) {}
