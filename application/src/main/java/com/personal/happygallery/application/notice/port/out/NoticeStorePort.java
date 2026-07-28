package com.personal.happygallery.application.notice.port.out;

import com.personal.happygallery.domain.notice.Notice;

public interface NoticeStorePort {

    Notice save(Notice notice);

    int incrementViewCountById(Long id);

    void deleteById(Long id);
}
