package com.personal.happygallery.adapter.out.persistence.dashboard.mapper;

import com.personal.happygallery.application.search.dto.AdminOrderSearchRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminOrderSearchMapper {

    List<AdminOrderSearchRow> search(
            @Param("status") String status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(
            @Param("status") String status,
            @Param("dateFrom") LocalDateTime dateFrom,
            @Param("dateTo") LocalDateTime dateTo,
            @Param("keyword") String keyword);
}
