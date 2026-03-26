package com.personal.happygallery.app.dashboard.port.out;

import com.personal.happygallery.app.dashboard.dto.SlotUtilization;
import java.time.LocalDate;
import java.util.List;

public interface BookingStatsQueryPort {

    List<SlotUtilization> findSlotUtilization(LocalDate from, LocalDate to);
}
