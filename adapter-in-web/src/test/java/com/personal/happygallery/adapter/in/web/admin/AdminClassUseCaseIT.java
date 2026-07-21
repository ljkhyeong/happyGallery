package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class AdminClassUseCaseIT {

    private static final String ADMIN_KEY = "dev-admin-key";

    @Autowired WebApplicationContext context;
    @Autowired ClassReaderPort classReaderPort;
    @Autowired TestCleanupSupport cleanupSupport;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        cleanupSupport.clearBookingData();
    }

    @DisplayName("관리자는 클래스 콘텐츠와 8회권 정책을 수정하고 운영을 중지할 수 있다")
    @Test
    void manageClassLifecycle_success() throws Exception {
        String created = mockMvc.perform(post("/api/v1/admin/classes")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "향수 원데이",
                                  "category": "perfume",
                                  "durationMin": 120,
                                  "price": 50000,
                                  "bufferMin": 30,
                                  "passEligible": false,
                                  "description": "향을 조합하는 원데이 클래스",
                                  "imageUrl": "/api/v1/media/images/class.webp",
                                  "preparationInfo": "편한 복장",
                                  "targetAudience": "향수 만들기가 처음인 분"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("PERFUME"))
                .andExpect(jsonPath("$.passEligible").value(false))
                .andExpect(jsonPath("$.description").value("향을 조합하는 원데이 클래스"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn().getResponse().getContentAsString();

        long classId = ((Number) com.jayway.jsonpath.JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(patch("/api/v1/admin/classes/{id}", classId)
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "우드 정규 클래스",
                                  "category": "wood",
                                  "price": 60000,
                                  "passEligible": true,
                                  "description": "나무를 다듬어 생활 소품을 만듭니다.",
                                  "preparationInfo": "앞치마",
                                  "targetAudience": "성인"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("우드 정규 클래스"))
                .andExpect(jsonPath("$.category").value("WOOD"))
                .andExpect(jsonPath("$.passEligible").value(true));

        mockMvc.perform(patch("/api/v1/admin/classes/{id}/status", classId)
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/admin/classes").header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("INACTIVE"));
        mockMvc.perform(get("/api/v1/classes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        assertThat(classReaderPort.findAll())
                .singleElement()
                .satisfies(bookingClass -> {
                    assertThat(bookingClass.getName()).isEqualTo("우드 정규 클래스");
                    assertThat(bookingClass.isPassEligible()).isTrue();
                    assertThat(bookingClass.getStatus()).isEqualTo(BookingClassStatus.INACTIVE);
                });
    }

    @DisplayName("관리자 키 없이 클래스 생성 API를 호출하면 401을 반환한다")
    @Test
    void createClass_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "향수 원데이",
                                  "category": "PERFUME",
                                  "durationMin": 120,
                                  "price": 50000,
                                  "bufferMin": 30,
                                  "passEligible": false
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
