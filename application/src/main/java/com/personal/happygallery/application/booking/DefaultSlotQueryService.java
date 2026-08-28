package com.personal.happygallery.application.booking;

import com.personal.happygallery.application.booking.port.in.SlotQueryUseCase;
import com.personal.happygallery.application.booking.port.out.ClassReaderPort;
import com.personal.happygallery.application.booking.port.out.SlotReaderPort;
import com.personal.happygallery.domain.booking.BookingClass;
import com.personal.happygallery.domain.booking.Slot;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DefaultSlotQueryService implements SlotQueryUseCase {

    private static final int MAX_UPCOMING_DAYS = 30;

    private final SlotReaderPort slotReaderPort;
    private final ClassReaderPort classReaderPort;
    private final BookingCalendarSlotMaterializer slotMaterializer;
    private final Clock clock;

    public DefaultSlotQueryService(SlotReaderPort slotReaderPort,
                                   ClassReaderPort classReaderPort,
                                   BookingCalendarSlotMaterializer slotMaterializer,
                                   Clock clock) {
        this.slotReaderPort = slotReaderPort;
        this.classReaderPort = classReaderPort;
        this.slotMaterializer = slotMaterializer;
        this.clock = clock;
    }

    /** 클래스 + 날짜 기준 예약 가능 슬롯 조회 */
    @Override
    public List<Slot> listAvailable(Long classId, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        BookingClass bookingClass = lockActiveClass(classId);
        return slotMaterializer.materialize(bookingClass, dayStart, dayEnd);
    }

    /** 오늘부터 지정한 일수 동안 예약 가능한 슬롯을 한 번에 조회한다. */
    @Override
    public List<Slot> listUpcoming(Long classId, int days) {
        return listUpcoming(classId, days, false);
    }

    @Override
    public List<Slot> listUpcoming(Long classId, int days, boolean includeFull) {
        if (days < 1 || days > MAX_UPCOMING_DAYS) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "조회 기간은 1일에서 30일 사이여야 합니다.");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime rangeStart = now.toLocalDate().atStartOfDay();
        LocalDateTime rangeEnd = rangeStart.plusDays(days);
        BookingClass bookingClass = lockActiveClass(classId);
        return includeFull
                ? slotMaterializer.materializeIncludingFull(bookingClass, rangeStart, rangeEnd)
                : slotMaterializer.materialize(bookingClass, rangeStart, rangeEnd);
    }

    /** 관리자용 — 클래스 기준 슬롯 전체 조회 (활성/비활성 포함) */
    @Override
    @Transactional(readOnly = true)
    public List<Slot> listByClass(Long classId) {
        return slotReaderPort.findByBookingClassIdOrderByStartAtDesc(classId);
    }

    private BookingClass lockActiveClass(Long classId) {
        BookingClass bookingClass = classReaderPort.findByIdForUpdate(classId)
                .orElseThrow(NotFoundException.supplier("클래스"));
        bookingClass.requireActive();
        return bookingClass;
    }
}
