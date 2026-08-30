package com.personal.happygallery.policy;

import com.personal.happygallery.domain.content.ContentTextPolicy;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.notice.Notice;
import com.personal.happygallery.domain.qna.ProductQna;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@Tag("policy")
class ContentTextPolicyTest {

    @DisplayName("문의와 Q&A와 공지는 저장 가능한 최대 길이의 본문을 허용한다")
    @Test
    void contentAggregates_acceptMaximumBodyLength() {
        String maximumBody = "가".repeat(ContentTextPolicy.MAX_BODY_LENGTH);

        Inquiry inquiry = new Inquiry(1L, "문의", maximumBody);
        ProductQna qna = new ProductQna(1L, 1L, "질문", maximumBody, false);
        Notice notice = new Notice("공지", maximumBody, false);

        assertThat(inquiry.getContent()).hasSize(ContentTextPolicy.MAX_BODY_LENGTH);
        assertThat(qna.getContent()).hasSize(ContentTextPolicy.MAX_BODY_LENGTH);
        assertThat(notice.getContent()).hasSize(ContentTextPolicy.MAX_BODY_LENGTH);
    }

    @DisplayName("문의와 Q&A와 공지는 TEXT 저장 한도를 넘는 본문을 거부한다")
    @Test
    void contentAggregates_rejectBodyOverMaximumLength() {
        String oversizedBody = "가".repeat(ContentTextPolicy.MAX_BODY_LENGTH + 1);

        assertInvalidInput(() -> new Inquiry(1L, "문의", oversizedBody));
        assertInvalidInput(() -> new ProductQna(1L, 1L, "질문", oversizedBody, false));
        assertInvalidInput(() -> new Notice("공지", oversizedBody, false));
    }

    @DisplayName("문의와 Q&A 답변은 저장 한도를 넘는 내용을 거부한다")
    @Test
    void answers_rejectBodyOverMaximumLength() {
        String oversizedBody = "가".repeat(ContentTextPolicy.MAX_BODY_LENGTH + 1);
        Inquiry inquiry = new Inquiry(1L, "문의", "문의 내용");
        ProductQna qna = new ProductQna(1L, 1L, "질문", "질문 내용", false);

        assertInvalidInput(() -> inquiry.reply(oversizedBody, 1L, null));
        assertInvalidInput(() -> qna.reply(oversizedBody, 1L, null));
    }

    @DisplayName("문의와 Q&A와 공지는 200자 제목을 생성과 공지 수정에서 허용한다")
    @Test
    void contentAggregates_acceptMaximumTitleLength() {
        String maximumTitle = "가".repeat(ContentTextPolicy.MAX_TITLE_LENGTH);

        Inquiry inquiry = new Inquiry(1L, maximumTitle, "문의 내용");
        ProductQna qna = new ProductQna(1L, 1L, maximumTitle, "질문 내용", false);
        Notice notice = new Notice("기존 공지", "공지 내용", false);
        notice.update(maximumTitle, "수정 내용", true);

        assertSoftly(softly -> {
            softly.assertThat(inquiry.getTitle()).hasSize(ContentTextPolicy.MAX_TITLE_LENGTH);
            softly.assertThat(qna.getTitle()).hasSize(ContentTextPolicy.MAX_TITLE_LENGTH);
            softly.assertThat(notice.getTitle()).hasSize(ContentTextPolicy.MAX_TITLE_LENGTH);
        });
    }

    @DisplayName("문의와 Q&A와 공지는 201자 제목과 공백 제목을 생성과 공지 수정에서 거절한다")
    @Test
    void contentAggregates_rejectInvalidTitles() {
        String oversizedTitle = "가".repeat(ContentTextPolicy.MAX_TITLE_LENGTH + 1);
        Notice notice = new Notice("기존 공지", "공지 내용", false);

        assertInvalidInput(() -> new Inquiry(1L, oversizedTitle, "문의 내용"));
        assertInvalidInput(() -> new ProductQna(1L, 1L, oversizedTitle, "질문 내용", false));
        assertInvalidInput(() -> new Notice(oversizedTitle, "공지 내용", false));
        assertInvalidInput(() -> notice.update(oversizedTitle, "수정 내용", true));
        assertInvalidInput(() -> new Inquiry(1L, "   ", "문의 내용"));
        assertInvalidInput(() -> new ProductQna(1L, 1L, "   ", "질문 내용", false));
        assertInvalidInput(() -> new Notice("   ", "공지 내용", false));
        assertInvalidInput(() -> notice.update("   ", "수정 내용", true));
    }

    private void assertInvalidInput(Runnable command) {
        assertThatThrownBy(command::run)
                .isInstanceOfSatisfying(HappyGalleryException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }
}
