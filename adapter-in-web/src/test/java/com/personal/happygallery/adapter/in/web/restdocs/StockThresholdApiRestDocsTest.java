package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.admin.AdminStockController;
import com.personal.happygallery.application.product.StockLevel;
import com.personal.happygallery.application.product.port.in.StockThresholdUseCase;
import com.personal.happygallery.domain.product.ProductType;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StockThresholdApiRestDocsTest extends RestDocsTestSupport {
    private MockMvc mvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider documentation) {
        var useCase = mock(StockThresholdUseCase.class);
        when(useCase.list(1L)).thenReturn(List.of(new StockLevel(1L, null, "재고 부족 상품",
                ProductType.READY_STOCK, 2, 3, 0, true)));
        mvc = mockMvc(documentation, new AdminStockController(useCase));
    }

    @Test
    @DisplayName("관리자 재고 기준 조회는 현재 수량과 부족 여부를 반환한다")
    void listStockLevels() throws Exception {
        mvc.perform(get("/api/v1/admin/stock-levels").param("productId", "1").with(adminUser()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].lowStock").value(true));
    }

    @Test
    @DisplayName("최소 보유 수량 저장과 해제 API를 문서화한다")
    void updateStockThreshold() throws Exception {
        mvc.perform(put("/api/v1/admin/stock-levels/threshold").with(adminUser())
                .contentType(APPLICATION_JSON).content("""
                        {"productId":1,"productVariantId":null,"minimumStock":3,"version":0}
                        """)).andExpect(status().isNoContent());
    }
}
