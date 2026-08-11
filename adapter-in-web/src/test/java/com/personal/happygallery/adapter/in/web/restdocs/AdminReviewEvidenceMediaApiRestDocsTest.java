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

import com.personal.happygallery.adapter.in.web.admin.AdminReviewEvidenceMediaController;
import com.personal.happygallery.adapter.in.web.security.admin.AdminPrincipal;
import com.personal.happygallery.application.review.port.in.ReviewEvidenceMediaUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

class AdminReviewEvidenceMediaApiRestDocsTest extends RestDocsTestSupport {

    private MockMvc mockMvc;
    private ReviewEvidenceMediaUseCase evidenceMediaUseCase;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        evidenceMediaUseCase = mock(ReviewEvidenceMediaUseCase.class);
        when(evidenceMediaUseCase.getImage(81L, 0))
                .thenReturn(new ReviewEvidenceMediaUseCase.EvidenceImageContent(
                        new byte[] {1, 2, 3}, "image/png"));
        mockMvc = mockMvc(
                restDocumentation,
                new AdminReviewEvidenceMediaController(evidenceMediaUseCase));
    }

    @Test
    @DisplayName("Bearer 관리자 세션으로 후기 증거 이미지를 조회한다")
    void get_review_evidence_image() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/admin/review-evidence/{evidenceId}/images/{sortOrder}",
                        81L,
                        0)
                        .with(adminUser()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    @DisplayName("계정 주체가 없는 로컬 API key는 후기 증거 이미지 조회를 거부한다")
    void reject_local_api_key_for_review_evidence_image() throws Exception {
        mockMvc.perform(get(
                        "/api/v1/admin/review-evidence/{evidenceId}/images/{sortOrder}",
                        81L,
                        0)
                        .with(localApiKey()))
                .andExpect(status().isForbidden());

        verify(evidenceMediaUseCase, never()).getImage(81L, 0);
    }

    private static RequestPostProcessor localApiKey() {
        return authentication(new TestingAuthenticationToken(
                AdminPrincipal.apiKey(), null, "ROLE_ADMIN"));
    }
}
