package com.personal.happygallery.adapter.out.persistence.notice;

import com.personal.happygallery.application.notice.port.out.NoticeReaderPort;
import com.personal.happygallery.domain.notice.Notice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long>, NoticeReaderPort {

    @Override Optional<Notice> findById(Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Notice n SET n.viewCount = n.viewCount + 1 WHERE n.id = :id")
    int incrementViewCountById(@Param("id") Long id);

    @Override List<Notice> findAllByOrderByPinnedDescCreatedAtDesc();

    @Query("SELECT n FROM Notice n ORDER BY n.pinned DESC, n.createdAt DESC")
    List<Notice> findRecentPage(Pageable pageable);

    @Override
    default List<Notice> findAllByOrderByPinnedDescCreatedAtDesc(int limit) {
        return findRecentPage(PageRequest.ofSize(limit));
    }
}
