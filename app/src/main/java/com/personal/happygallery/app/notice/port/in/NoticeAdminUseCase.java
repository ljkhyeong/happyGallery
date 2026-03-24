package com.personal.happygallery.app.notice.port.in;

import com.personal.happygallery.domain.notice.Notice;

public interface NoticeAdminUseCase {

    Notice create(String title, String content, boolean pinned);

    Notice update(Long id, String title, String content, boolean pinned);

    void delete(Long id);
}
