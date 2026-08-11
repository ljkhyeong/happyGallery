package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.admin.dto.CreateNoticeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.InquiryReplyRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.QnaReplyRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateNoticeRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CreateInquiryRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CreateQnaRequest;
import com.personal.happygallery.adapter.in.web.review.dto.CreateClassReviewRequest;
import com.personal.happygallery.adapter.in.web.review.dto.CreateProductReviewRequest;
import com.personal.happygallery.adapter.in.web.review.dto.CreateReviewReportRequest;
import com.personal.happygallery.adapter.in.web.review.dto.DecideReviewReportRequest;
import com.personal.happygallery.adapter.in.web.review.dto.ReviewReportDecision;
import com.personal.happygallery.adapter.in.web.review.dto.UpdateReviewRequest;
import com.personal.happygallery.adapter.in.web.review.dto.UpsertReviewReplyRequest;
import com.personal.happygallery.domain.content.ContentTextPolicy;
import com.personal.happygallery.domain.review.ReviewReport;
import com.personal.happygallery.domain.review.ReviewReportReason;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentRequestValidationTest {

    @DisplayName("문의와 Q&A와 공지와 후기 요청은 저장 가능한 최대 길이의 본문을 허용한다")
    @Test
    void contentRequests_acceptMaximumBodyLength() {
        String maximumBody = "가".repeat(ContentTextPolicy.MAX_BODY_LENGTH);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(contentRequests(maximumBody))
                    .allSatisfy(request -> assertThat(validator.validate(request)).isEmpty());
        }
    }

    @DisplayName("문의와 Q&A와 공지와 후기 요청은 저장 한도를 넘는 본문을 검증 오류로 거절한다")
    @Test
    void contentRequests_rejectBodyOverMaximumLength() {
        String oversizedBody = "가".repeat(ContentTextPolicy.MAX_BODY_LENGTH + 1);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(contentRequests(oversizedBody))
                    .allSatisfy(request -> assertThat(validator.validate(request)).hasSize(1));
        }
    }

    @DisplayName("문의와 Q&A와 공지 요청은 200자 제목을 허용하고 201자 제목을 거절한다")
    @Test
    void contentRequests_enforceTitleLengthBoundary() {
        String maximumTitle = "가".repeat(ContentTextPolicy.MAX_TITLE_LENGTH);
        String oversizedTitle = maximumTitle + "가";

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(titledContentRequests(maximumTitle))
                    .allSatisfy(request -> assertThat(validator.validate(request)).isEmpty());
            assertThat(titledContentRequests(oversizedTitle))
                    .allSatisfy(request -> assertThat(validator.validate(request)).hasSize(1));
        }
    }

    @DisplayName("문의와 Q&A와 공지 요청은 공백 제목을 거절한다")
    @Test
    void contentRequests_rejectBlankTitle() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(titledContentRequests("   "))
                    .allSatisfy(request -> assertThat(validator.validate(request)).isNotEmpty());
        }
    }

    @DisplayName("후기 요청은 1점과 5점을 허용하고 범위를 벗어난 별점을 거절한다")
    @Test
    void reviewRequests_enforceRatingBoundary() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(List.of(
                    new CreateProductReviewRequest(1L, 1, "후기 내용"),
                    new CreateClassReviewRequest(1L, 5, "후기 내용"),
                    new UpdateReviewRequest(1L, 5, "후기 내용")))
                    .allSatisfy(request -> assertThat(validator.validate(request)).isEmpty());
            assertThat(List.of(
                    new CreateProductReviewRequest(1L, 0, "후기 내용"),
                    new CreateClassReviewRequest(1L, 6, "후기 내용"),
                    new UpdateReviewRequest(1L, 0, "후기 내용")))
                    .allSatisfy(request -> assertThat(validator.validate(request)).hasSize(1));
        }
    }

    @DisplayName("후기 신고 상세와 처리 메모는 각각 저장 가능한 최대 길이만 허용한다")
    @Test
    void reviewReportRequests_enforceDetailLengthBoundary() {
        String maximumDetail = "가".repeat(ReviewReport.MAX_DETAIL_LENGTH);
        String maximumDecisionNote = "나".repeat(ReviewReport.MAX_DECISION_NOTE_LENGTH);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(validator.validate(new CreateReviewReportRequest(
                    ReviewReportReason.OTHER, maximumDetail))).isEmpty();
            assertThat(validator.validate(new DecideReviewReportRequest(
                    ReviewReportDecision.ACCEPTED, maximumDecisionNote))).isEmpty();
            assertThat(validator.validate(new CreateReviewReportRequest(
                    ReviewReportReason.OTHER, maximumDetail + "가"))).hasSize(1);
            assertThat(validator.validate(new DecideReviewReportRequest(
                    ReviewReportDecision.REJECTED, maximumDecisionNote + "나"))).hasSize(1);
        }
    }

    private List<Object> contentRequests(String body) {
        return List.of(
                new CreateInquiryRequest("문의", body),
                new CreateQnaRequest("질문", body, false),
                new InquiryReplyRequest(body),
                new QnaReplyRequest(body),
                new CreateNoticeRequest("공지", body, false),
                new UpdateNoticeRequest(0L, "공지", body, false),
                new CreateProductReviewRequest(1L, 5, body),
                new CreateClassReviewRequest(1L, 5, body),
                new UpdateReviewRequest(1L, 5, body),
                new UpsertReviewReplyRequest(0L, body));
    }

    private List<Object> titledContentRequests(String title) {
        return List.of(
                new CreateInquiryRequest(title, "문의 내용"),
                new CreateQnaRequest(title, "질문 내용", false),
                new CreateNoticeRequest(title, "공지 내용", false),
                new UpdateNoticeRequest(0L, title, "공지 내용", false));
    }
}
