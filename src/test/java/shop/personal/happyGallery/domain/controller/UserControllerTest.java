package shop.personal.happyGallery.domain.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
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

import shop.personal.happyGallery.dto.UserResponseDto;
import shop.personal.happyGallery.model.embeded.Address;
import shop.personal.happyGallery.service.UserService;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UserControllerTest.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class UserControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	UserService userService;

	@Test
	@DisplayName("유저 찾기")
	void find() throws Exception {
		UserResponseDto dto = new UserResponseDto("test@naver.com",
			new Address("Seoul", "04330", "Guro", "etc"));

		given(userService.findUser(1L)).willReturn(dto);

		mockMvc.perform(get("/api/v1/users/{id}", 1L)
					.accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andDo(document("findUser",
					// preprocessResponse(prettyPrint()),
					pathParameters(
						parameterWithName("id").description("유저 ID")
					),
					responseFields(
						fieldWithPath("code").description("결과 코드(SUCCESS/FAILED)"),
						subsectionWithPath("data").description("유저 정보")
					).andWithPrefix("data.",
						fieldWithPath("email").description("이메일"),
						fieldWithPath("address").description("주소")
						)
					));
	}
}
