package com.personal.happygallery.app.booking.port.in;

import com.personal.happygallery.domain.booking.Slot;
import java.time.LocalDate;
import java.util.List;

/**
 * 슬롯 조회 유스케이스.
 */
public interface SlotQueryUseCase {

    List<Slot> listAvailable(Long classId, LocalDate date);

    List<Slot> listByClass(Long classId);
}
