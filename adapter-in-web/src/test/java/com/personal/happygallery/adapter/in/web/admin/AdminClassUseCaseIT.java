package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.AdminAuthFilter;
import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class AdminClassUseCaseIT {

    private static final String ADMIN_KEY = "dev-admin-key";

    @Autowired WebApplicationContext context;
    @Autowired AdminAuthFilter adminAuthFilter;
    @Autowired ClassReaderPort classReaderPort;
    @Autowired TestCleanupSupport cleanupSupport;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(adminAuthFilter)
                .build();
        cleanupSupport.clearBookingData();
    }

    @DisplayName("관리자 클래스 생성이 성공한다")
    @Test
    void createClass_success() throws Exception {
        mockMvc.perform(post("/admin/classes")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "향수 원데이",
                                  "category": "perfume",
                                  "durationMin": 120,
                                  "price": 50000,
                                  "bufferMin": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("향수 원데이"))
                .andExpect(jsonPath("$.category").value("PERFUME"))
                .andExpect(jsonPath("$.durationMin").value(120))
                .andExpect(jsonPath("$.price").value(50000))
                .andExpect(jsonPath("$.bufferMin").value(30));

        assertThat(classReaderPort.findAll())
                .singleElement()
                .extracting(BookingClass::getName, BookingClass::getCategory)
                .containsExactly("향수 원데이", "PERFUME");
    }

    @DisplayName("관리자 키 없이 클래스 생성 API를 호출하면 401을 반환한다")
    @Test
    void createClass_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/admin/classes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "향수 원데이",
                                  "category": "PERFUME",
                                  "durationMin": 120,
                                  "price": 50000,
                                  "bufferMin": 30
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
