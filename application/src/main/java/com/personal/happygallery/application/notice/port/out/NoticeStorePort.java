package com.personal.happygallery.application.notice.port.out;

import com.personal.happygallery.domain.notice.Notice;

public interface NoticeStorePort {

    <S extends Notice> S save(S notice);

    int incrementViewCountById(Long id);

    void deleteById(Long id);
}
