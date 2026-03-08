package com.personal.happygallery.app.booking;

import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.infra.booking.SlotRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SlotQueryService {

    private final SlotRepository slotRepository;

    public SlotQueryService(SlotRepository slotRepository) {
        this.slotRepository = slotRepository;
    }

    /** 클래스 + 날짜 기준 예약 가능 슬롯 조회 */
    public List<Slot> listAvailable(Long classId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(LocalTime.MAX);
        return slotRepository.findAvailableByClassAndDate(classId, dayStart, dayEnd);
    }
}
