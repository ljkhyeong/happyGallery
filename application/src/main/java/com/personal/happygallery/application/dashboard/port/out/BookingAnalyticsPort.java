package com.personal.happygallery.application.dashboard.port.out;

import com.personal.happygallery.application.dashboard.dto.SlotUtilization;
import java.time.LocalDate;
import java.util.List;

public interface BookingAnalyticsPort {

    List<SlotUtilization> findSlotUtilization(LocalDate from, LocalDate to);
}
