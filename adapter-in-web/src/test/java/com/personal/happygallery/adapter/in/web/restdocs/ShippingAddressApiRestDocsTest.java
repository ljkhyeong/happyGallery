package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.MeOrderCustomerActionController;
import com.personal.happygallery.adapter.in.web.order.OrderCustomerActionController;
import com.personal.happygallery.application.order.port.in.OrderCustomerActionUseCase;
import com.personal.happygallery.application.order.port.in.OrderShippingAddressUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ShippingAddressApiRestDocsTest extends RestDocsTestSupport {
    private MockMvc mvc;
    private OrderShippingAddressUseCase addresses;
    private static final String REQUEST = """
            {"version":0,"shippingAddress":{"recipientName":"수령인","phone":"01012345678",
            "postalCode":"12345","addressLine1":"서울시 변경 주소 12","addressLine2":"201호"}}
            """;

    @BeforeEach
    void setUp(RestDocumentationContextProvider documentation) {
        addresses = mock(OrderShippingAddressUseCase.class);
        var actions = mock(OrderCustomerActionUseCase.class);
        mvc = mockMvc(documentation, new MeOrderCustomerActionController(actions, addresses),
                new OrderCustomerActionController(actions, addresses));
    }

    @Test
    @DisplayName("회원 배송지 수정 API는 로그인 회원과 조회 버전을 전달한다")
    void updateMemberAddress() throws Exception {
        mvc.perform(put("/api/v1/me/orders/200/shipping-address").with(customerUser())
                .contentType(APPLICATION_JSON).content(REQUEST)).andExpect(status().isNoContent());
        verify(addresses).updateMember(eq(200L), eq(CUSTOMER_USER_ID), eq(0L), any());
    }

    @Test
    @DisplayName("비회원 배송지 수정 API는 조회 코드와 조회 버전을 전달한다")
    void updateGuestAddress() throws Exception {
        mvc.perform(put("/api/v1/orders/200/shipping-address").header("X-Access-Token", "guest-token")
                .contentType(APPLICATION_JSON).content(REQUEST)).andExpect(status().isNoContent());
        verify(addresses).updateGuest(eq(200L), eq("guest-token"), eq(0L), any());
    }

    @Test
    @DisplayName("배송지 수정은 조회 버전 누락을 거절한다")
    void rejectMissingVersion() throws Exception {
        mvc.perform(put("/api/v1/me/orders/200/shipping-address").with(customerUser())
                .contentType(APPLICATION_JSON).content(REQUEST.replace("\"version\":0,", "")))
                .andExpect(status().isBadRequest());
    }
}
