package com.personal.happygallery.adapter.out.persistence.dashboard.mapper;

import com.personal.happygallery.application.search.port.out.AdminPassQueryResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminPassQueryMapper {

    List<AdminPassQueryResult> search(
            @Param("keyword") String keyword,
            @Param("nameHmac") String nameHmac,
            @Param("phoneHmac") String phoneHmac,
            @Param("exactId") Long exactId,
            @Param("now") LocalDateTime now,
            @Param("offset") int offset,
            @Param("size") int size);

    long count(
            @Param("keyword") String keyword,
            @Param("nameHmac") String nameHmac,
            @Param("phoneHmac") String phoneHmac,
            @Param("exactId") Long exactId);

    Optional<AdminPassQueryResult> findById(
            @Param("passId") Long passId,
            @Param("now") LocalDateTime now);
}
