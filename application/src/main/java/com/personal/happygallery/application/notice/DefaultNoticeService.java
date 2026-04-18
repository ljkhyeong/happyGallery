package com.personal.happygallery.application.notice;

import com.personal.happygallery.application.notice.port.in.NoticeAdminUseCase;
import com.personal.happygallery.application.notice.port.in.NoticeQueryUseCase;
import com.personal.happygallery.application.notice.port.out.NoticeReaderPort;
import com.personal.happygallery.application.notice.port.out.NoticeStorePort;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notice.Notice;
import java.util.List;
import org.springframework.data.domain.PageRequest;
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
    @Transactional(readOnly = true)
    public List<Notice> listRecent(int limit) {
        return noticeReader.findAllByOrderByPinnedDescCreatedAtDesc(PageRequest.of(0, limit));
    }

    @Override
    @Transactional
    public Notice getDetail(Long id) {
        Notice notice = noticeReader.findById(id)
                .orElseThrow(NotFoundException.supplier("공지사항"));
        notice.incrementViewCount();
        return noticeStore.save(notice);
    }

    @Override
    @Transactional
    public Notice create(String title, String content, boolean pinned) {
        return noticeStore.save(new Notice(title, content, pinned));
    }

    @Override
    @Transactional
    public Notice update(Long id, String title, String content, boolean pinned) {
        Notice notice = noticeReader.findById(id)
                .orElseThrow(NotFoundException.supplier("공지사항"));
        notice.update(title, content, pinned);
        return noticeStore.save(notice);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        noticeReader.findById(id)
                .orElseThrow(NotFoundException.supplier("공지사항"));
        noticeStore.deleteById(id);
    }
}
