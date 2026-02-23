package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class AdminSlotUseCaseIT {

    @Autowired WebApplicationContext context;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired BookingRepository bookingRepository;

    MockMvc mockMvc;
    Long classId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
        bookingHistoryRepository.deleteAll();
        bookingRepository.deleteAll();
        slotRepository.deleteAll();
        classRepository.deleteAll();
        BookingClass cls = classRepository.save(
                new BookingClass("향수 클래스", "PERFUME", 120, 50_000L, 30));
        classId = cls.getId();
    }

    @Test
    void createSlot_success() throws Exception {
        mockMvc.perform(post("/admin/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": %d,
                                  "startAt": "2026-03-01T10:00:00",
                                  "endAt":   "2026-03-01T12:00:00"
                                }
                                """.formatted(classId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classId").value(classId))
                .andExpect(jsonPath("$.capacity").value(8))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void deactivateSlot_success() throws Exception {
        // given — 슬롯 생성
        String response = mockMvc.perform(post("/admin/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": %d,
                                  "startAt": "2026-03-02T10:00:00",
                                  "endAt":   "2026-03-02T12:00:00"
                                }
                                """.formatted(classId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long slotId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.id")).longValue();

        // when — 비활성화
        mockMvc.perform(patch("/admin/slots/{id}/deactivate", slotId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // then — DB 상태 확인
        assertThat(slotRepository.findById(slotId))
                .isPresent()
                .hasValueSatisfying(s -> assertThat(s.isActive()).isFalse());
    }

    @Test
    void createSlot_notFoundClass() throws Exception {
        mockMvc.perform(post("/admin/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": 99999,
                                  "startAt": "2026-03-01T10:00:00",
                                  "endAt":   "2026-03-01T12:00:00"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void createSlot_duplicateStartAt() throws Exception {
        String body = """
                {
                  "classId": %d,
                  "startAt": "2026-03-03T10:00:00",
                  "endAt":   "2026-03-03T12:00:00"
                }
                """.formatted(classId);

        // 첫 번째 생성 — 성공
        mockMvc.perform(post("/admin/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // 두 번째 동일 시간 — 실패
        mockMvc.perform(post("/admin/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
