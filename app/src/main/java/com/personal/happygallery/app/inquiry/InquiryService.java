package com.personal.happygallery.app.inquiry;

import com.personal.happygallery.app.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.inquiry.port.out.InquiryReaderPort;
import com.personal.happygallery.app.inquiry.port.out.InquiryStorePort;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InquiryService implements InquiryUseCase {

    private final InquiryReaderPort inquiryReader;
    private final InquiryStorePort inquiryStore;
    private final UserReaderPort userReader;
    private final Clock clock;

    public InquiryService(InquiryReaderPort inquiryReader,
                          InquiryStorePort inquiryStore,
                          UserReaderPort userReader,
                          Clock clock) {
        this.inquiryReader = inquiryReader;
        this.inquiryStore = inquiryStore;
        this.userReader = userReader;
        this.clock = clock;
    }

    @Transactional
    public Inquiry create(Long userId, String title, String content) {
        return inquiryStore.save(new Inquiry(userId, title, content));
    }

    @Transactional(readOnly = true)
    public List<Inquiry> listByUser(Long userId) {
        return inquiryReader.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Inquiry findByIdAndUser(Long inquiryId, Long userId) {
        Inquiry inquiry = inquiryReader.findById(inquiryId)
                .orElseThrow(() -> new NotFoundException("문의"));
        if (!inquiry.getUserId().equals(userId)) {
            throw new NotFoundException("문의");
        }
        return inquiry;
    }

    @Transactional(readOnly = true)
    public List<InquiryWithUser> listAll() {
        List<Inquiry> inquiries = inquiryReader.findAll();
        Map<Long, User> userMap = batchFetchUsers(inquiries);
        return inquiries.stream()
                .map(i -> new InquiryWithUser(i, userName(userMap, i.getUserId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public InquiryWithUser findByIdForAdmin(Long inquiryId) {
        Inquiry inquiry = inquiryReader.findById(inquiryId)
                .orElseThrow(() -> new NotFoundException("문의"));
        String name = userReader.findById(inquiry.getUserId())
                .map(User::getName).orElse("탈퇴회원");
        return new InquiryWithUser(inquiry, name);
    }

    @Transactional
    public Inquiry reply(Long inquiryId, String replyContent, Long adminId) {
        Inquiry inquiry = inquiryReader.findById(inquiryId)
                .orElseThrow(() -> new NotFoundException("문의"));
        inquiry.reply(replyContent, adminId, LocalDateTime.now(clock));
        return inquiryStore.save(inquiry);
    }

    private Map<Long, User> batchFetchUsers(List<Inquiry> inquiries) {
        List<Long> userIds = inquiries.stream().map(Inquiry::getUserId).distinct().toList();
        return userReader.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private static String userName(Map<Long, User> userMap, Long userId) {
        User user = userMap.get(userId);
        return user != null ? user.getName() : "탈퇴회원";
    }

    public record InquiryWithUser(Inquiry inquiry, String userName) {}
}
