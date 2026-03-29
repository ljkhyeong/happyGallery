package com.personal.happygallery.infra.notice;

import com.personal.happygallery.app.notice.port.out.NoticeReaderPort;
import com.personal.happygallery.app.notice.port.out.NoticeStorePort;
import com.personal.happygallery.domain.notice.Notice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, Long>, NoticeReaderPort, NoticeStorePort {

    @Override Optional<Notice> findById(Long id);
    @Override Notice save(Notice notice);
    @Override void deleteById(Long id);

    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();

    List<Notice> findAllByOrderByPinnedDescCreatedAtDesc(Pageable pageable);
}
