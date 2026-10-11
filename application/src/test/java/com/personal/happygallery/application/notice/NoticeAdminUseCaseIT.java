package com.personal.happygallery.application.notice;

import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class NoticeAdminUseCaseIT {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "notices");
    }

    @Test
    @DisplayName("관리자가 공지를 등록하면 DB가 정한 작성 시각을 담아 201로 응답한다")
    void create_respondsWithDatabaseCreatedAt() throws Exception {
        mockMvc.perform(post("/api/v1/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "10월 휴무 안내",
                                  "content": "10월 9일은 공방을 쉽니다.",
                                  "pinned": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.version").value(0))
                .andExpect(jsonPath("$.createdAt").isString());
    }
}
