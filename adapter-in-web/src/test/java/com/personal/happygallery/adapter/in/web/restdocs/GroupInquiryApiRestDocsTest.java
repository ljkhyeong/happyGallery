package com.personal.happygallery.adapter.in.web.restdocs;

import com.personal.happygallery.adapter.in.web.customer.MeGroupInquiryController;
import com.personal.happygallery.adapter.in.web.inquiry.GroupInquiryController;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.application.shared.page.CursorPage;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupInquiryApiRestDocsTest extends RestDocsTestSupport {
    private MockMvc mvc;
    @BeforeEach
    void setUp(RestDocumentationContextProvider documentation) {
        var useCase = mock(GroupInquiryUseCase.class);
        var view = GroupInquiryRestDocsFixtures.detail().view();
        when(useCase.create(isNull(), any())).thenReturn(view);
        when(useCase.create(eq(CUSTOMER_USER_ID), any())).thenReturn(view);
        when(useCase.listForMember(CUSTOMER_USER_ID, null, 20)).thenReturn(new CursorPage<>(List.of(view), null, false));
        var memberDetail = new GroupInquiryUseCase.MemberDetail(view, List.of());
        when(useCase.detailForMember(CUSTOMER_USER_ID, 51L)).thenReturn(memberDetail);
        when(useCase.reviseByMember(CUSTOMER_USER_ID, 51L, 0, 30, "10월 오전")).thenReturn(memberDetail);
        when(useCase.cancelByMember(CUSTOMER_USER_ID, 51L, 0)).thenReturn(memberDetail);
        mvc = mockMvc(documentation, new GroupInquiryController(useCase), new MeGroupInquiryController(useCase));
    }
    @Test
    @DisplayName("비회원 단체 문의 접수 번호와 상태를 문서화한다")
    void guest_create() throws Exception {
        mvc.perform(post("/api/v1/group-inquiries").contentType(APPLICATION_JSON).content(GroupInquiryRestDocsFixtures.REQUEST))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(51));
    }
    @Test
    @DisplayName("로그인 회원의 단체 문의 접수를 문서화한다")
    void member_create() throws Exception {
        mvc.perform(post("/api/v1/me/group-inquiries").with(customerUser())
                        .contentType(APPLICATION_JSON).content(GroupInquiryRestDocsFixtures.REQUEST))
                .andExpect(status().isCreated());
    }
    @Test
    @DisplayName("회원 본인의 단체 문의 목록은 상담 메모와 연락처를 포함하지 않는다")
    void member_list() throws Exception {
        mvc.perform(get("/api/v1/me/group-inquiries").with(customerUser()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].organization").value("충주 기관"))
                .andExpect(jsonPath("$.content[0].phone").doesNotExist())
                .andExpect(jsonPath("$.content[0].activities").doesNotExist());
    }

    @Test
    @DisplayName("회원 문의 상세는 버전과 본인 변경 이력을 반환하고 관리자 연락일과 메모는 노출하지 않는다")
    void member_detail() throws Exception {
        mvc.perform(get("/api/v1/me/group-inquiries/51").with(customerUser()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.changes").isArray()).andExpect(jsonPath("$.activities").doesNotExist())
                .andExpect(jsonPath("$.nextContactOn").doesNotExist());
    }

    @Test
    @DisplayName("회원 문의 수정과 취소 요청은 읽은 버전을 필수로 받는다")
    void member_update_and_cancel() throws Exception {
        mvc.perform(put("/api/v1/me/group-inquiries/51").with(customerUser()).contentType(APPLICATION_JSON)
                        .content("{\"version\":0,\"headcount\":30,\"preferredSchedule\":\"10월 오전\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/me/group-inquiries/51/cancel").with(customerUser()).contentType(APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/me/group-inquiries/51").with(customerUser()).contentType(APPLICATION_JSON)
                        .content("{\"headcount\":30,\"preferredSchedule\":\"10월 오전\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/me/group-inquiries/51/cancel").with(customerUser()).contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
