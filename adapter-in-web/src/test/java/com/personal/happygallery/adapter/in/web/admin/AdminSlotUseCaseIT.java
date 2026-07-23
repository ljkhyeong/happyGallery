package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.CreateSlotRequest;
import com.personal.happygallery.adapter.in.web.admin.dto.BulkSlotRequest;
import com.personal.happygallery.application.booking.port.out.ClassStorePort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotStorePort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.BookingClassStatus;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.ObjectMapper;

import static com.personal.happygallery.support.TestFixtures.defaultBookingClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @Autowired ObjectMapper objectMapper;

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

    @DisplayName("관리자 슬롯 생성이 성공한다")
    @Test
    void createSlot_success() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotRequest(classId, LocalDateTime.of(2026, 3, 2, 10, 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.classId").value(classId))
                .andExpect(jsonPath("$.endAt").value("2026-03-02T12:00:00"))
                .andExpect(jsonPath("$.capacity").value(8))
                .andExpect(jsonPath("$.adminActive").value(true))
                .andExpect(jsonPath("$.bufferBlocked").value(false))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @DisplayName("관리자 슬롯 목록 조회는 비활성 슬롯을 포함해 시작 시각 내림차순으로 반환한다")
    @Test
    void listSlots_includingInactiveOrderedByStartAtDesc() throws Exception {
        String firstResponse = mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotRequest(classId, LocalDateTime.of(2026, 3, 2, 10, 0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotRequest(classId, LocalDateTime.of(2026, 3, 3, 10, 0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long firstSlotId = ((Number) com.jayway.jsonpath.JsonPath.read(firstResponse, "$.id")).longValue();
        long secondSlotId = ((Number) com.jayway.jsonpath.JsonPath.read(secondResponse, "$.id")).longValue();

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
        // given — 슬롯 생성
        String response = mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotRequest(classId, LocalDateTime.of(2026, 3, 2, 10, 0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        long slotId = ((Number) com.jayway.jsonpath.JsonPath.read(response, "$.id")).longValue();

        // when — 비활성화
        mockMvc.perform(patch("/api/v1/admin/slots/{id}/deactivate", slotId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));

        // then — DB 상태 확인
        assertThat(slotReaderPort.findById(slotId))
                .isPresent()
                .hasValueSatisfying(slot -> assertThat(slot.isActive()).isFalse());

        mockMvc.perform(patch("/api/v1/admin/slots/{id}/activate", slotId)
                        .header("X-Admin-Key", ADMIN_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminActive").value(true))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @DisplayName("존재하지 않는 클래스로 슬롯을 생성하면 실패한다")
    @Test
    void createSlot_notFoundClass() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotRequest(99999L, LocalDateTime.of(2026, 3, 1, 10, 0))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @DisplayName("동일 클래스에 같은 시작 시각 슬롯을 생성하면 실패한다")
    @Test
    void createSlot_duplicateStartAt() throws Exception {
        String body = slotRequest(classId, LocalDateTime.of(2026, 3, 3, 10, 0));

        // 첫 번째 생성 — 성공
        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        // 두 번째 동일 시간 — 실패
        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @DisplayName("슬롯 일괄 미리보기와 생성은 과거와 중복을 건너뛰고 예약 버퍼를 반영한다")
    @Test
    void previewAndCreateBulkSlots_reportsEachCandidate() throws Exception {
        Slot bookedSource = new Slot(
                bookingClass,
                LocalDateTime.of(2026, 3, 2, 7, 0),
                LocalDateTime.of(2026, 3, 2, 9, 0));
        bookedSource.incrementBookedCount();
        slotStorePort.save(bookedSource);
        slotStorePort.save(new Slot(bookingClass, LocalDateTime.of(2026, 3, 3, 14, 0)));
        String request = objectMapper.writeValueAsString(new BulkSlotRequest(
                classId,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 3),
                Set.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                Set.of(LocalTime.of(9, 0), LocalTime.of(14, 0))));

        mockMvc.perform(post("/api/v1/admin/slots/bulk/preview")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(6))
                .andExpect(jsonPath("$.creatableCount").value(4))
                .andExpect(jsonPath("$.skippedCount").value(2))
                .andExpect(jsonPath("$.items[0].status").value("SKIPPED_PAST"))
                .andExpect(jsonPath("$.items[2].bufferBlocked").value(true))
                .andExpect(jsonPath("$.items[5].status").value("SKIPPED_DUPLICATE"));

        mockMvc.perform(post("/api/v1/admin/slots/bulk")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount").value(4))
                .andExpect(jsonPath("$.skippedCount").value(2))
                .andExpect(jsonPath("$.items[1].status").value("CREATED"));

        assertThat(slotReaderPort.findByBookingClassIdOrderByStartAtDesc(classId)).hasSize(6);
    }

    @DisplayName("운영 중지 클래스에는 새 슬롯을 생성할 수 없다")
    @Test
    void createSlot_forInactiveClass_returns422() throws Exception {
        bookingClass.changeStatus(BookingClassStatus.INACTIVE);
        classStorePort.save(bookingClass);

        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", ADMIN_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotRequest(classId, LocalDateTime.of(2026, 3, 2, 10, 0))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("CLASS_INACTIVE"));
    }

    @DisplayName("관리자 키 없이 관리자 API를 호출하면 401을 반환한다")
    @Test
    void callAdminWithoutKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotRequest(classId, LocalDateTime.of(2026, 3, 10, 10, 0))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("잘못된 관리자 키로 관리자 API를 호출하면 401을 반환한다")
    @Test
    void callAdminWithWrongKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/slots")
                        .header("X-Admin-Key", "wrong-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(slotRequest(classId, LocalDateTime.of(2026, 3, 11, 10, 0))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private String slotRequest(Long requestedClassId, LocalDateTime startAt) throws Exception {
        return objectMapper.writeValueAsString(new CreateSlotRequest(requestedClassId, startAt));
    }
}
