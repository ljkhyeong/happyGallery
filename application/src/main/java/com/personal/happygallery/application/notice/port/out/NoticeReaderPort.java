package com.personal.happygallery.application.notice.port.out;

import com.personal.happygallery.domain.notice.Notice;
import java.util.List;
import java.util.Optional;

public interface NoticeReaderPort {

    Optional<Notice> findById(Long id);

    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();

    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc(int limit);
}
