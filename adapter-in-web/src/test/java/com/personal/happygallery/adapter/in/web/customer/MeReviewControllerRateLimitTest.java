package com.personal.happygallery.adapter.in.web.customer;

import com.personal.happygallery.adapter.in.web.ratelimit.RateLimitUnavailableException;
import com.personal.happygallery.adapter.in.web.ratelimit.SubjectRateLimitGuard;
import com.personal.happygallery.adapter.in.web.review.dto.CreateReviewReportRequest;
import com.personal.happygallery.adapter.in.web.security.customer.CustomerPrincipal;
import com.personal.happygallery.application.review.port.in.MemberReviewUseCase;
import com.personal.happygallery.application.review.port.in.ReviewInteractionUseCase;
import com.personal.happygallery.domain.review.ReviewReportReason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MeReviewControllerRateLimitTest {

    private static final CustomerPrincipal CUSTOMER = new CustomerPrincipal(
            11L, "member@example.com", "회원", "01012345678", true, true, 0L);

    @DisplayName("후기 신고 subject 제한이 닫히면 신고를 저장하지 않는다")
    @Test
    void report_doesNotReachUseCaseWhenSubjectLimitUnavailable() {
        MemberReviewUseCase memberReviewUseCase = mock(MemberReviewUseCase.class);
        ReviewInteractionUseCase reviewInteractionUseCase = mock(ReviewInteractionUseCase.class);
        SubjectRateLimitGuard guard = mock(SubjectRateLimitGuard.class);
        doThrow(new RateLimitUnavailableException()).when(guard).checkReviewReport(11L);
        MeReviewController controller = new MeReviewController(
                memberReviewUseCase, reviewInteractionUseCase, guard);

        assertThatThrownBy(() -> controller.report(
                31L,
                new CreateReviewReportRequest(ReviewReportReason.SPAM, null),
                CUSTOMER))
                .isInstanceOf(RateLimitUnavailableException.class);

        verifyNoInteractions(memberReviewUseCase, reviewInteractionUseCase);
    }

    @DisplayName("후기 이미지 subject 제한이 닫히면 multipart 파일도 읽지 않는다")
    @Test
    void imageUpload_doesNotReadFileWhenSubjectLimitUnavailable() {
        MemberReviewUseCase memberReviewUseCase = mock(MemberReviewUseCase.class);
        ReviewInteractionUseCase reviewInteractionUseCase = mock(ReviewInteractionUseCase.class);
        SubjectRateLimitGuard guard = mock(SubjectRateLimitGuard.class);
        MultipartFile file = mock(MultipartFile.class);
        doThrow(new RateLimitUnavailableException()).when(guard).checkReviewImageUpload(11L);
        MeReviewController controller = new MeReviewController(
                memberReviewUseCase, reviewInteractionUseCase, guard);

        assertThatThrownBy(() -> controller.addImage(31L, file, CUSTOMER))
                .isInstanceOf(RateLimitUnavailableException.class);

        verifyNoInteractions(file, memberReviewUseCase, reviewInteractionUseCase);
    }
}
