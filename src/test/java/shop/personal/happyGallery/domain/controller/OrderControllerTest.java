package shop.personal.happyGallery.domain.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.request.RequestDocumentation;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import shop.personal.happyGallery.controller.OrderController;
import shop.personal.happyGallery.dto.ItemDto;
import shop.personal.happyGallery.dto.OrderResponseDto;
import shop.personal.happyGallery.model.embeded.Money;
import shop.personal.happyGallery.model.enums.OrderStatus;
import shop.personal.happyGallery.service.OrderService;

@ExtendWith(SpringExtension.class)
@WebMvcTest(OrderController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class OrderControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	OrderService orderService;

	@Test
	void 주문정보_가져오기() throws Exception {
		ItemDto item = new ItemDto(1L, "티셔츠", Money.of(10000L), 1);
		OrderResponseDto dto = new OrderResponseDto(1L, 1L, OrderStatus.PLACED, List.of(item), Money.of(10000L));

		given(orderService.getOrderInfo(1L)).willReturn(dto);

		mockMvc.perform(get("/api/v1/orders/{id}", 1L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("get-order-info",
				pathParameters(
					parameterWithName("id").description("주문 ID")
				),
				responseFields(
					fieldWithPath("code").description("결과 코드"),
					subsectionWithPath("data").description("주문 정보")
				).andWithPrefix("data.",
					fieldWithPath("id").description("주문 ID"),
					fieldWithPath("userId").description("유저 ID"),
					fieldWithPath("orderStatus").description("주문 상태"),
					fieldWithPath("totalPrice").description("상품 가격"),
					subsectionWithPath("items").description("주문 상품들")
				).andWithPrefix("data.items[].",
					fieldWithPath("productId").description("상품 ID"),
					fieldWithPath("name").description("상품명"),
					fieldWithPath("price").description("상품 가격"),
					fieldWithPath("quantity").description("상품 재고")
				)
			));
	}

	@Test
	void 장바구니로부터_주문_생성() throws Exception {
		ItemDto item = new ItemDto(1L, "티셔츠", Money.of(10000L), 1);
		OrderResponseDto dto = new OrderResponseDto(1L, 1L, OrderStatus.PLACED, List.of(item), Money.of(10000L));

		given(orderService.createOrderFromCart(1L)).willReturn(dto);

		mockMvc.perform(post("/api/v1/orders")
				.param("userId", "1")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("create-order-from-cart",
				requestFields(
					fieldWithPath("id").description("유저 ID")
				),
				responseFields(
					fieldWithPath("code").description("결과 코드"),
					subsectionWithPath("data").description("주문 정보")
				).andWithPrefix("data.",
					fieldWithPath("id").description("주문 ID"),
					fieldWithPath("userId").description("유저 ID"),
					fieldWithPath("orderStatus").description("주문 상태"),
					subsectionWithPath("totalPrice").description("상품 가격"),
					subsectionWithPath("items").description("주문 상품들")
				).andWithPrefix("data.items[].",
					fieldWithPath("productId").description("상품 ID"),
					fieldWithPath("name").description("상품명"),
					fieldWithPath("price").description("상품 가격"),
					fieldWithPath("quantity").description("상품 재고")
				)
			));
	}

}
