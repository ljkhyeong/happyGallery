package com.personal.happygallery.application.inquiry;

import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.inquiry.port.out.InquiryReaderPort;
import com.personal.happygallery.application.inquiry.port.out.InquiryStorePort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

@Service
public class DefaultInquiryService implements InquiryUseCase {

    private final InquiryReaderPort inquiryReader;
    private final InquiryStorePort inquiryStore;
    private final UserReaderPort userReader;
    private final Clock clock;

    public DefaultInquiryService(InquiryReaderPort inquiryReader,
                                 InquiryStorePort inquiryStore,
                                 UserReaderPort userReader,
                                 Clock clock) {
        this.inquiryReader = inquiryReader;
        this.inquiryStore = inquiryStore;
        this.userReader = userReader;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Inquiry create(Long userId, String title, String content) {
        return inquiryStore.save(new Inquiry(userId, title, content));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Inquiry> listByUser(Long userId) {
        return inquiryReader.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Inquiry findByIdAndUser(Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryReader.findById(inquiryId)
                .orElseThrow(NotFoundException.supplier("문의"));
        if (!inquiry.getUserId().equals(userId)) {
            throw new NotFoundException("문의");
        }
        return inquiry;
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<InquiryWithUser> listAll(String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        int fetchSize = pageSize + 1;
        List<Inquiry> inquiries;
        if (cursor == null) {
            inquiries = inquiryReader.findRecent(fetchSize);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            inquiries = inquiryReader.findRecentAfter(
                    cursorParam.timestamp(), cursorParam.id(), fetchSize);
        }
        Map<Long, User> userMap = batchFetchUsers(inquiries);
        List<InquiryWithUser> items = inquiries.stream()
                .map(i -> new InquiryWithUser(i, userName(userMap, i.getUserId())))
                .toList();
        return CursorPage.of(items, pageSize, item -> CursorUtils.encode(
                item.inquiry().getCreatedAt(), item.inquiry().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public InquiryWithUser findByIdForAdmin(Long inquiryId) {
        Inquiry inquiry = inquiryReader.findById(inquiryId)
                .orElseThrow(NotFoundException.supplier("문의"));
        String name = userReader.findById(inquiry.getUserId())
                .map(User::getName).orElse("탈퇴회원");
        return new InquiryWithUser(inquiry, name);
    }

    @Override
    @Transactional
    public InquiryWithUser replyAndGet(Long inquiryId, String replyContent, Long adminId) {
        Inquiry inquiry = inquiryReader.findById(inquiryId)
                .orElseThrow(NotFoundException.supplier("문의"));
        inquiry.reply(replyContent, adminId, LocalDateTime.now(clock));
        inquiry = inquiryStore.save(inquiry);
        String name = userReader.findById(inquiry.getUserId())
                .map(User::getName).orElse("탈퇴회원");
        return new InquiryWithUser(inquiry, name);
    }

    private Map<Long, User> batchFetchUsers(List<Inquiry> inquiries) {
        List<Long> userIds = inquiries.stream().map(Inquiry::getUserId).distinct().toList();
        return userReader.findAllById(userIds).stream()
                .collect(toMap(User::getId, Function.identity()));
    }

    private static String userName(Map<Long, User> userMap, Long userId) {
        User user = userMap.get(userId);
        return user != null ? user.getName() : "탈퇴회원";
    }

}
