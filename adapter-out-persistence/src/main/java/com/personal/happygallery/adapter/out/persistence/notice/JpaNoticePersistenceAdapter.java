package com.personal.happygallery.adapter.out.persistence.notice;

import com.personal.happygallery.application.notice.port.out.NoticeStorePort;
import com.personal.happygallery.domain.notice.Notice;
import org.springframework.stereotype.Repository;

@Repository
class JpaNoticePersistenceAdapter implements NoticeStorePort {

    private final NoticeRepository repository;

    JpaNoticePersistenceAdapter(NoticeRepository repository) {
        this.repository = repository;
    }

    @Override
    public Notice save(Notice notice) {
        return repository.save(notice);
    }

    @Override
    public int incrementViewCountById(Long id) {
        return repository.incrementViewCountById(id);
    }

    @Override
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
