package com.personal.happygallery.application.notice.port.out;

import com.personal.happygallery.domain.notice.Notice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface NoticeReaderPort {

    Optional<Notice> findById(Long id);

    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();

    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc(Pageable pageable);
}
