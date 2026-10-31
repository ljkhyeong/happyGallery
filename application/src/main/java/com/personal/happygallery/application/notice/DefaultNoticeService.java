package com.personal.happygallery.application.notice;

import com.personal.happygallery.application.notice.port.in.NoticeAdminUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.application.notice.port.out.NoticeReaderPort;
import com.personal.happygallery.application.notice.port.out.NoticeStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notice.Notice;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultNoticeService implements NoticeQueryUseCase, NoticeAdminUseCase {

    private final NoticeReaderPort noticeReader;
    private final NoticeStorePort noticeStore;

    public DefaultNoticeService(NoticeReaderPort noticeReader, NoticeStorePort noticeStore) {
        this.noticeReader = noticeReader;
        this.noticeStore = noticeStore;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notice> listAll() {
        return noticeReader.findAllByOrderByPinnedDescCreatedAtDesc();
    }

    @Override
    @Transactional
    public Notice getDetail(Long id) {
        if (noticeStore.incrementViewCountById(id) == 0) {
            throw new NotFoundException("공지사항");
        }
        return noticeReader.findById(id)
                .orElseThrow(NotFoundException.supplier("공지사항"));
    }

    @Override
    @Transactional(readOnly = true)
    public Notice getForEdit(Long id) {
        return noticeReader.findById(id)
                .orElseThrow(NotFoundException.supplier("공지사항"));
    }

    @Override
    @Transactional
    public Notice create(String title, String content, boolean pinned) {
        return noticeStore.save(new Notice(title, content, pinned));
    }

    @Override
    @Transactional
    public Notice update(Long id, long expectedVersion, String title, String content, boolean pinned) {
        Notice notice = noticeReader.findById(id)
                .orElseThrow(NotFoundException.supplier("공지사항"));
        notice.requireVersion(expectedVersion);
        notice.update(title, content, pinned);
        return noticeStore.save(notice);
    }

    @Override
    @Transactional
    public void delete(Long id, long expectedVersion) {
        Notice notice = noticeReader.findById(id)
                .orElseThrow(NotFoundException.supplier("공지사항"));
        notice.requireVersion(expectedVersion);
        noticeStore.deleteById(id);
    }
}
