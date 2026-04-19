package com.personal.happygallery.adapter.out.persistence.dashboard.mapper;

import com.personal.happygallery.application.dashboard.dto.SlotUtilization;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BookingStatsMapper {

    List<SlotUtilization> findSlotUtilization(@Param("rangeFrom") LocalDateTime rangeFrom,
                                              @Param("rangeTo") LocalDateTime rangeTo);
}
