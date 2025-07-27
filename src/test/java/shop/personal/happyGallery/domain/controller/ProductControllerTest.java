package shop.personal.happyGallery.domain.controller;

import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import shop.personal.happyGallery.controller.ProductController;
import shop.personal.happyGallery.dto.ProductRegisterRequestDto;
import shop.personal.happyGallery.dto.ProductResponseDto;
import shop.personal.happyGallery.model.embeded.Money;
import shop.personal.happyGallery.service.ProductService;

@ExtendWith(SpringExtension.class)
@WebMvcTest(ProductController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class ProductControllerTest {

	@Autowired
	MockMvc mockMvc;
	@MockitoBean
	ProductService productService;

	@Test
	@DisplayName("상품 조회")
	void detail() throws Exception {
		ProductResponseDto dto = new ProductResponseDto(1L, null, "티셔츠", "티셔츠임",
			Money.of(10000L), Money.of(10000L), 50);

		given(productService.getProduct(1L)).willReturn(dto);

		mockMvc.perform(get("/api/v1/products/{productId}", 1L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("product",
				// preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("id").description("상품ID")
				),
				responseFields(
					fieldWithPath("code").description("결과 코드"),
					subsectionWithPath("data").description("상품 정보")
				).andWithPrefix("data.",
					fieldWithPath("id").description("상품 ID"),
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("description").description("상품 설명"),
					fieldWithPath("price").description("상품 가격"),
					fieldWithPath("realPrice").description("상품 실제 가격"),
					fieldWithPath("stock").description("상품 재고"))
			));
	}

	@Test
	@DisplayName("카테고리별 상품 목록 조회")
	void productList() throws Exception {
		List<ProductResponseDto> list = List.of(
			new ProductResponseDto(1L, null, "티셔츠", "티셔츠임", Money.of(10000L), Money.of(10000L), 50),
			new ProductResponseDto(1L, null, "바지", "바지임", Money.of(5000L), Money.of(5000L), 20)
		);

		given(productService.getProductsByCategory(1L)).willReturn(list);

		mockMvc.perform(get("/api/v1/categories/{id}/products", 1L)
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andDo(document("product-list",
				preprocessResponse(prettyPrint()),
				pathParameters(
					parameterWithName("id").description("카테고리 ID")
				),
				responseFields(
					fieldWithPath("code").description("결과 코드(SUCCESS/FAIL)"),
					subsectionWithPath("data[]").description("상품 리스트")
				).andWithPrefix("data[].",
					fieldWithPath("id").description("상품 ID"),
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("description").description("상품 설명"),
					fieldWithPath("price").description("상품 가격"),
					fieldWithPath("realPrice").description("상품 실제 가격"),
					fieldWithPath("stock").description("상품 재고"))
			));
	}

	@Test
	@DisplayName("상품 생성")
	void createProduct() throws Exception {
		ProductRegisterRequestDto req = new ProductRegisterRequestDto("티셔츠", "티셔츠임", Money.of(10000L), 10);

		mockMvc.perform(post("/api/v1/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(new ObjectMapper().writeValueAsString(req)))
			.andExpect(status().isOk())
			.andDo(document("product-create",
				requestFields(
					fieldWithPath("name").description("상품 이름"),
					fieldWithPath("description").description("상품 설명"),
					fieldWithPath("price").description("상품 가격"),
					fieldWithPath("stock").description("상품 재고")
				)
			));
	}

	@Test
	@DisplayName("상품 제거")
	void deleteProduct() throws Exception {
		mockMvc.perform(delete("/api/v1/products/{id}", 1L))
			.andExpect(status().isOk())
			.andDo(document("product-delete",
				pathParameters(
					parameterWithName("id").description("상품 ID")
				)
			));
	}
}
