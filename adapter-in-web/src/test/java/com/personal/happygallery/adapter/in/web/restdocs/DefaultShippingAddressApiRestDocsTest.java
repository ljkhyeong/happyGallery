package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.MeDefaultShippingAddressController;
import com.personal.happygallery.application.customer.port.in.DefaultShippingAddressUseCase;
import com.personal.happygallery.domain.order.ShippingAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DefaultShippingAddressApiRestDocsTest extends RestDocsTestSupport {
    private MockMvc mvc;
    @BeforeEach
    void setUp(RestDocumentationContextProvider documentation) {
        var useCase = mock(DefaultShippingAddressUseCase.class);
        when(useCase.get(CUSTOMER_USER_ID)).thenReturn(new DefaultShippingAddressUseCase.View(1,
                new ShippingAddress("수령인", "01012345678", "12345", "서울시 테스트로 10", null)));
        mvc = mockMvc(documentation, new MeDefaultShippingAddressController(useCase));
    }

    @Test
    @DisplayName("기본 배송지를 변경 번호와 함께 조회한다")
    void getAddress() throws Exception {
        mvc.perform(get("/api/v1/me/default-shipping-address").with(customerUser()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.shippingAddress.recipientName").value("수령인"));
    }

    @Test
    @DisplayName("현재 변경 번호로 기본 배송지를 저장하고 삭제한다")
    void saveAndDelete() throws Exception {
        mvc.perform(put("/api/v1/me/default-shipping-address").with(customerUser()).contentType(APPLICATION_JSON)
                        .content(""" 
                                {"version":1,"shippingAddress":{"recipientName":"수령인","phone":"01012345678","postalCode":"12345","addressLine1":"서울시 테스트로 10","addressLine2":null}}
                                """))
                .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/me/default-shipping-address").with(customerUser()).param("version", "2"))
                .andExpect(status().isNoContent());
    }
}
