package com.personal.happygallery.app.web.admin;

import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.infra.booking.BookingHistoryRepository;
import com.personal.happygallery.infra.booking.BookingRepository;
import com.personal.happygallery.infra.booking.ClassRepository;
import com.personal.happygallery.infra.booking.SlotRepository;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static com.personal.happygallery.support.TestDataCleaner.clearBookingData;
import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
@AutoConfigureMockMvc(addFilters = true)
class AdminSlotUseCaseIT {

    private static final String ADMIN_KEY = "dev-admin-key";

    @Autowired MockMvc mockMvc;
    @Autowired ClassRepository classRepository;
    @Autowired SlotRepository slotRepository;
    @Autowired BookingHistoryRepository bookingHistoryRepository;
    @Autowired BookingRepository bookingRepository;

    Long classId;

    @BeforeEach
    void setUp() {
        clearBookingData(bookingHistoryRepository, bookingRepository, slotRepository, classRepository);
        BookingClass cls = classRepository.save(defaultBookingClass());
        classId = cls.getId();
    }

    @DisplayName("관리자 슬롯 생성이 성공한다")
    @Test
    void createSlot_success() throws Exception {
        mockMvc.perform(post("/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
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

    @DisplayName("관리자 슬롯 목록 조회는 비활성 슬롯을 포함해 시작 시각 내림차순으로 반환한다")
    @Test
    void listSlots_includingInactiveOrderedByStartAtDesc() throws Exception {
        String firstResponse = mockMvc.perform(post("/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": %d,
                                  "startAt": "2026-03-01T10:00:00",
                                  "endAt":   "2026-03-01T12:00:00"
                                }
                                """.formatted(classId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
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

        long firstSlotId = ((Number) com.jayway.jsonpath.JsonPath.read(firstResponse, "$.id")).longValue();
        long secondSlotId = ((Number) com.jayway.jsonpath.JsonPath.read(secondResponse, "$.id")).longValue();

        mockMvc.perform(patch("/admin/slots/{id}/deactivate", firstSlotId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .param("classId", classId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(secondSlotId))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[1].id").value(firstSlotId))
                .andExpect(jsonPath("$[1].isActive").value(false));
    }

    @DisplayName("관리자 슬롯 비활성화가 성공한다")
    @Test
    void deactivateSlot_success() throws Exception {
        // given — 슬롯 생성
        String response = mockMvc.perform(post("/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
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
        mockMvc.perform(patch("/admin/slots/{id}/deactivate", slotId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // then — DB 상태 확인
        assertThat(slotRepository.findById(slotId))
                .isPresent()
                .hasValueSatisfying(s -> assertThat(s.isActive()).isFalse());
    }

    @DisplayName("존재하지 않는 클래스로 슬롯을 생성하면 실패한다")
    @Test
    void createSlot_notFoundClass() throws Exception {
        mockMvc.perform(post("/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
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

    @DisplayName("동일 클래스에 같은 시작 시각 슬롯을 생성하면 실패한다")
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
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // 두 번째 동일 시간 — 실패
        mockMvc.perform(post("/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @DisplayName("관리자 키 없이 관리자 API를 호출하면 401을 반환한다")
    @Test
    void callAdminWithoutKey_returns401() throws Exception {
        mockMvc.perform(post("/admin/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": %d,
                                  "startAt": "2026-03-10T10:00:00",
                                  "endAt":   "2026-03-10T12:00:00"
                                }
                                """.formatted(classId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("잘못된 관리자 키로 관리자 API를 호출하면 401을 반환한다")
    @Test
    void callAdminWithWrongKey_returns401() throws Exception {
        mockMvc.perform(post("/admin/slots")
                        .header("X-Admin-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "classId": %d,
                                  "startAt": "2026-03-11T10:00:00",
                                  "endAt":   "2026-03-11T12:00:00"
                                }
                                """.formatted(classId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
