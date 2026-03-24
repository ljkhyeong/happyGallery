package com.personal.happygallery.app.notice.port.in;

import com.personal.happygallery.domain.notice.Notice;
import java.util.List;

public interface NoticeQueryUseCase {

    List<Notice> listAll();

    List<Notice> listRecent(int limit);

    Notice getDetail(Long id);
}
