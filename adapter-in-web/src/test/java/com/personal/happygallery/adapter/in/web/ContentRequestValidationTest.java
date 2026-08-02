package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.admin.dto.CreateNoticeRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.InquiryReplyRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.QnaReplyRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.UpdateNoticeRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CreateInquiryRequest;
import com.personal.happygallery.adapter.in.web.customer.dto.CreateQnaRequest;
import com.personal.happygallery.domain.content.ContentTextPolicy;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentRequestValidationTest {

    @DisplayName("문의와 Q&A와 공지 요청은 저장 가능한 최대 길이의 본문을 허용한다")
    @Test
    void contentRequests_acceptMaximumBodyLength() {
        String maximumBody = "가".repeat(ContentTextPolicy.MAX_BODY_LENGTH);

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertThat(contentRequests(maximumBody))
                    .allSatisfy(request -> assertThat(validator.validate(request)).isEmpty());
        }
    }

    @DisplayName("문의와 Q&A와 공지 요청은 저장 한도를 넘는 본문을 검증 오류로 거절한다")
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

    private List<Object> contentRequests(String body) {
        return List.of(
                new CreateInquiryRequest("문의", body),
                new CreateQnaRequest("질문", body, false),
                new InquiryReplyRequest(body),
                new QnaReplyRequest(body),
                new CreateNoticeRequest("공지", body, false),
                new UpdateNoticeRequest(0L, "공지", body, false));
    }

    private List<Object> titledContentRequests(String title) {
        return List.of(
                new CreateInquiryRequest(title, "문의 내용"),
                new CreateQnaRequest(title, "질문 내용", false),
                new CreateNoticeRequest(title, "공지 내용", false),
                new UpdateNoticeRequest(0L, title, "공지 내용", false));
    }
}
