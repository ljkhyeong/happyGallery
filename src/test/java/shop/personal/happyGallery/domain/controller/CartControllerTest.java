package shop.personal.happyGallery.domain.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import shop.personal.happyGallery.controller.CartController;
import shop.personal.happyGallery.dto.CartItemAddRequestDto;
import shop.personal.happyGallery.dto.CartItemChangeRequestDto;
import shop.personal.happyGallery.dto.CartResponseDto;
import shop.personal.happyGallery.dto.ItemDto;
import shop.personal.happyGallery.model.embeded.Money;
import shop.personal.happyGallery.service.CartService;

@ExtendWith(SpringExtension.class)
@WebMvcTest(CartController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class CartControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	CartService cartService;

	@Test
	void 장바구니_가져오기() throws Exception {
		ItemDto item = new ItemDto(1L, "티셔츠", Money.of(10000L), 1);
		CartResponseDto dto = new CartResponseDto(1L, 1L, List.of(item));

		given(cartService.getCart(1L)).willReturn(dto);

		mockMvc.perform(get("/api/v1/users/{userId}/cart", 1L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("get-cart",
				pathParameters(
					parameterWithName("userId").description("유저 ID")
				),
				responseFields(
					fieldWithPath("code").description("결과 코드"),
					subsectionWithPath("data").description("카트 정보")
				).andWithPrefix("data.",
					fieldWithPath("id").description("카트 ID"),
					fieldWithPath("userId").description("유저 ID"),
					subsectionWithPath("items").description("장바구니 내 상품들")
				).andWithPrefix("data.items[].",
					fieldWithPath("productId").description("상품 ID"),
					fieldWithPath("name").description("상품명"),
					fieldWithPath("price").description("상품 가격"),
					fieldWithPath("quantity").description("상품 재고")
				)
			));
	}

	@Test
	void 장바구니_아이템_추가() throws Exception {
		ItemDto item = new ItemDto(1L, "티셔츠", Money.of(10000L), 1);
		CartResponseDto response = new CartResponseDto(1L, 1L, List.of(item));
		CartItemAddRequestDto request = new CartItemAddRequestDto(1L, 1);

		given(cartService.addItem(1L, request.productId(), request.quantity())).willReturn(response);

		mockMvc.perform(post("/api/v1/users/{userId}/cart/items", 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(document("cart-add-item",
				pathParameters(
					parameterWithName("userId").description("유저 ID")
				),
				requestFields(
					fieldWithPath("productId").description("상품 ID"),
					fieldWithPath("quantity").description("상품 수량")
				),
				responseFields(
					fieldWithPath("code").description("결과 코드"),
					subsectionWithPath("data").description("카트 정보")
				).andWithPrefix("data.",
					fieldWithPath("id").description("카트 ID"),
					fieldWithPath("userId").description("유저 ID"),
					subsectionWithPath("items").description("장바구니 내 상품들")
				).andWithPrefix("data.items[].",
					fieldWithPath("productId").description("상품 ID"),
					fieldWithPath("name").description("상품명"),
					fieldWithPath("price").description("상품 가격"),
					fieldWithPath("quantity").description("상품 재고")
				)
			));
	}

	@Test
	void 장바구니_수량_변경() throws Exception {
		ItemDto item = new ItemDto(1L, "티셔츠", Money.of(10000L), 2);
		CartResponseDto response = new CartResponseDto(1L, 1L, List.of(item));
		CartItemChangeRequestDto request = new CartItemChangeRequestDto(2);

		given(cartService.changeQuantity(1L, 1L, request.quantity())).willReturn(response);

		mockMvc.perform(put("/api/v1/users/{userId}/cart/items/{productId}", 1L, 1L)
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(request)))
			.andExpect(status().isOk())
			.andDo(document("cart-change-quantity",
				pathParameters(
					parameterWithName("userId").description("유저 ID"),
					parameterWithName("productId").description("상품 ID")
				),
				requestFields(
					fieldWithPath("quantity").description("변경할 수량")
				),
				responseFields(
					fieldWithPath("code").description("결과 코드"),
					subsectionWithPath("data").description("카트 정보")
				).andWithPrefix("data.",
					fieldWithPath("id").description("카트 ID"),
					fieldWithPath("userId").description("유저 ID"),
					subsectionWithPath("items").description("장바구니 내 상품들")
				).andWithPrefix("data.items[].",
					fieldWithPath("productId").description("상품 ID"),
					fieldWithPath("name").description("상품명"),
					fieldWithPath("price").description("상품 가격"),
					fieldWithPath("quantity").description("상품 재고")
				)
			));
	}

	@Test
	void 장바구니_비우기() throws Exception {
		mockMvc.perform(delete("/api/v1/users/{userId}/cart/items", 1L))
			.andExpect(status().isNoContent())
			.andDo(document("cart-clear",
				pathParameters(
					parameterWithName("userId").description("유저 ID")
				)
			));
	}
}
