package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.MeRestockAlertController;
import com.personal.happygallery.application.product.port.in.RestockAlertUseCase;
import com.personal.happygallery.domain.product.RestockAlert;
import com.personal.happygallery.domain.product.RestockAlertStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RestockAlertApiRestDocsTest extends RestDocsTestSupport {
    private MockMvc mvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider documentation) {
        var useCase = mock(RestockAlertUseCase.class);
        var alert = mock(RestockAlert.class);
        when(alert.getId()).thenReturn(10L);
        when(alert.getProductId()).thenReturn(42L);
        when(alert.getProductVariantId()).thenReturn(50L);
        when(alert.getOptionLabel()).thenReturn("색상: 빨강");
        when(alert.getStatus()).thenReturn(RestockAlertStatus.WAITING);
        when(alert.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 9, 5, 10, 0));
        when(useCase.list(CUSTOMER_USER_ID)).thenReturn(List.of(new RestockAlertUseCase.View(alert, "가죽 키링")));
        mvc = mockMvc(documentation, new MeRestockAlertController(useCase));
    }

    @Test
    @DisplayName("회원 재입고 알림 목록에 상품과 신청 옵션을 표시한다")
    void listAlerts() throws Exception {
        mvc.perform(get("/api/v1/me/restock-alerts").with(customerUser()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].optionLabel").value("색상: 빨강"));
    }

    @Test
    @DisplayName("회원은 상품과 옵션 조합으로 재입고 알림을 신청한다")
    void registerAlert() throws Exception {
        mvc.perform(post("/api/v1/me/restock-alerts").with(customerUser())
                .contentType(APPLICATION_JSON).content("{\"productId\":42,\"productVariantId\":50}"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("회원은 본인 재입고 알림을 해지한다")
    void cancelAlert() throws Exception {
        mvc.perform(delete("/api/v1/me/restock-alerts/10").with(customerUser()))
                .andExpect(status().isNoContent());
    }
}
