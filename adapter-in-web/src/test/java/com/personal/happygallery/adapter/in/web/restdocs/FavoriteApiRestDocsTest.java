package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.MeFavoriteController;
import com.personal.happygallery.application.customer.port.in.FavoriteUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.user.FavoriteTargetType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FavoriteApiRestDocsTest extends RestDocsTestSupport {
    private MockMvc mvc;
    @BeforeEach
    void setUp(RestDocumentationContextProvider documentation) {
        var useCase = mock(FavoriteUseCase.class);
        when(useCase.isSaved(CUSTOMER_USER_ID, FavoriteTargetType.PRODUCT, 42L)).thenReturn(true);
        when(useCase.list(CUSTOMER_USER_ID, null, null, 20)).thenReturn(new CursorPage<>(List.of(
                new FavoriteUseCase.View(1L, FavoriteTargetType.PRODUCT, 42L, "찜 상품", true, LocalDateTime.of(2026, 9, 5, 10, 0))), null, false));
        mvc = mockMvc(documentation, new MeFavoriteController(useCase));
    }
    @Test
    @DisplayName("회원 찜 목록과 상품 저장 여부를 조회한다")
    void listAndStatus() throws Exception {
        mvc.perform(get("/api/v1/me/favorites").with(customerUser())).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].targetType").value("PRODUCT"));
        mvc.perform(get("/api/v1/me/favorites/PRODUCT/42").with(customerUser())).andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));
    }
    @Test
    @DisplayName("회원은 상품과 클래스를 찜하고 해제한다")
    void saveAndRemove() throws Exception {
        mvc.perform(put("/api/v1/me/favorites/CLASS/42").with(customerUser())).andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/me/favorites/CLASS/42").with(customerUser())).andExpect(status().isNoContent());
    }
}
