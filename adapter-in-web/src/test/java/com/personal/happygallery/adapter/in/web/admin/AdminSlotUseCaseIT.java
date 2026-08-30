package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@UseCaseIT
class AdminSlotUseCaseIT {

    private static final String ADMIN_KEY = "dev-admin-key";

    @Autowired WebApplicationContext context;
    @Autowired ClassStorePort classStorePort;
    @Autowired SlotReaderPort slotReaderPort;
    @Autowired SlotStorePort slotStorePort;
    @Autowired TestCleanupSupport cleanupSupport;

    MockMvc mockMvc;
    Long classId;
    BookingClass bookingClass;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        bookingClass = classStorePort.save(defaultBookingClass());
        classId = bookingClass.getId();
    }

    @AfterEach
    void tearDown() {
        cleanupSupport.clearBookingData();
    }

    @DisplayName("공개 슬롯 조회는 기본 운영시간의 예약 회차를 자동으로 준비한다")
    @Test
    void listAvailableSlots_materializesDefaultOpenCalendar() throws Exception {
        mockMvc.perform(get("/api/v1/slots")
                        .param("classId", classId.toString())
                        .param("date", "2026-03-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15))
                .andExpect(jsonPath("$[0].startAt").value("2026-03-03T10:00:00"))
                .andExpect(jsonPath("$[14].startAt").value("2026-03-03T17:00:00"));
    }

    @DisplayName("기본 차단된 공휴일도 관리자가 날짜를 열면 예약할 수 있다")
    @Test
    void openPublicHoliday_enablesAutomaticSlots() throws Exception {
        mockMvc.perform(get("/api/v1/slots")
                        .param("classId", classId.toString())
                        .param("date", "2026-03-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(put("/api/v1/admin/slots/calendar/days/{date}", "2026-03-02")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mode\":\"OPEN\",\"reason\":\"공휴일 특별 운영\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/slots")
                        .param("classId", classId.toString())
                        .param("date", "2026-03-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(15));
    }

    @DisplayName("관리자가 닫은 시간과 수업 시간이 겹치는 회차는 공개하지 않는다")
    @Test
    void createTimeBlock_hidesOverlappingSessions() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots/calendar/time-blocks")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-03-03",
                                  "startTime": "12:00",
                                  "endTime": "13:00",
                                  "reason": "점심시간"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("점심시간"));

        mockMvc.perform(get("/api/v1/slots")
                        .param("classId", classId.toString())
                        .param("date", "2026-03-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].startAt").value("2026-03-03T10:00:00"))
                .andExpect(jsonPath("$[1].startAt").value("2026-03-03T13:00:00"));
    }

    @DisplayName("관리자 슬롯 목록 조회는 비활성 슬롯을 포함해 시작 시각 내림차순으로 반환한다")
    @Test
    void listSlots_includingInactiveOrderedByStartAtDesc() throws Exception {
        long firstSlotId = slotStorePort.save(
                new Slot(bookingClass, LocalDateTime.of(2026, 3, 2, 10, 0))).getId();
        long secondSlotId = slotStorePort.save(
                new Slot(bookingClass, LocalDateTime.of(2026, 3, 3, 10, 0))).getId();

        mockMvc.perform(patch("/api/v1/admin/slots/{id}/deactivate", firstSlotId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .param("classId", classId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(secondSlotId))
                .andExpect(jsonPath("$[0].isActive").value(true))
                .andExpect(jsonPath("$[1].id").value(firstSlotId))
                .andExpect(jsonPath("$[1].isActive").value(false));
    }

    @DisplayName("관리자가 슬롯을 비활성화한 뒤 다시 활성화할 수 있다")
    @Test
    void deactivateAndActivateSlot_success() throws Exception {
        long slotId = slotStorePort.save(
                new Slot(bookingClass, LocalDateTime.of(2026, 3, 2, 10, 0))).getId();

        // when — 비활성화
        mockMvc.perform(patch("/api/v1/admin/slots/{id}/deactivate", slotId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // then — DB 상태 확인
        assertThat(slotReaderPort.findById(slotId))
                .hasValueSatisfying(slot -> assertThat(slot.isActive()).isFalse());

        mockMvc.perform(patch("/api/v1/admin/slots/{id}/activate", slotId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminActive").value(true))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @DisplayName("관리자 키 없이 관리자 API를 호출하면 401을 반환한다")
    @Test
    void callAdminWithoutKey_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/slots")
                        .param("classId", classId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("잘못된 관리자 키로 관리자 API를 호출하면 401을 반환한다")
    @Test
    void callAdminWithWrongKey_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/slots")
                        .header("X-Admin-Key", "wrong-key")
                        .param("classId", classId.toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
