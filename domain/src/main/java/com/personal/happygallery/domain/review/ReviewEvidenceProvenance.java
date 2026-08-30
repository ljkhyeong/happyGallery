package com.personal.happygallery.domain.review;

/** 후기 증거가 생성된 경로. 과거 신고 backfill은 사진 완전성을 보장할 수 없다. */
public enum ReviewEvidenceProvenance {
    LIVE,
    LEGACY_REPORT
}
