package com.personal.happygallery.adapter.in.web.restdocs;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.personal.happygallery.adapter.in.web.admin.AdminReviewImageMediaController;
import com.personal.happygallery.adapter.in.web.customer.MeReviewImageMediaController;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;
import com.personal.happygallery.application.review.port.in.ReviewImageMediaUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class ReviewImageMediaApiRestDocsTest extends RestDocsTestSupport {

    private MockMvc mockMvc;
    private ReviewImageMediaUseCase reviewImageMediaUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        reviewImageMediaUseCase = mock(ReviewImageMediaUseCase.class);
        when(reviewImageMediaUseCase.getOwnedImage(CUSTOMER_USER_ID, 71L, 81L))
                .thenReturn(new ImageContent(
                        new byte[] {1, 2, 3}, "image/png"));
        when(reviewImageMediaUseCase.getAdminImage(71L, 81L))
                .thenReturn(new ImageContent(
                        new byte[] {4, 5, 6}, "image/jpeg"));
        mockMvc = mockMvc(
                restDocumentation,
                new MeReviewImageMediaController(reviewImageMediaUseCase),
                new AdminReviewImageMediaController(reviewImageMediaUseCase));
    }

    @Test
    @DisplayName("회원은 자신의 숨김 후기 이미지를 고객 세션 보호 경로로 조회한다")
    void get_owned_hidden_review_image() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/me/reviews/{reviewId}/images/{imageId}",
                        71L,
                        81L)
                        .with(customerUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    @DisplayName("Bearer 관리자는 숨김 후기 이미지를 관리자 보호 경로로 조회한다")
    void get_admin_hidden_review_image() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/admin/reviews/{reviewId}/images/{imageId}",
                        71L,
                        81L)
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(new byte[] {4, 5, 6}));
    }

    @Test
    @DisplayName("계정 주체가 없는 로컬 API key는 숨김 후기 이미지 조회를 거부한다")
    void reject_local_api_key_for_hidden_review_image() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/admin/reviews/{reviewId}/images/{imageId}",
                        71L,
                        81L)
                        .with(localApiKey()))
                .andExpect(status().isForbidden());

        verify(reviewImageMediaUseCase, never()).getAdminImage(71L, 81L);
    }

    private static RequestPostProcessor localApiKey() {
        return authentication(new TestingAuthenticationToken(
                AdminPrincipal.apiKey(), null, "ROLE_ADMIN"));
    }
}
