package com.personal.happygallery.app.notice.port.out;

import com.personal.happygallery.domain.notice.Notice;

public interface NoticeStorePort {

    Notice save(Notice notice);

    void deleteById(Long id);
}
