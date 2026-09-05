package com.personal.happygallery.application.inquiry;

import com.personal.happygallery.domain.crypto.FieldEncryptor;
import com.personal.happygallery.application.customer.MemberAccountGuard;
import com.personal.happygallery.application.inquiry.port.in.GroupInquiryUseCase;
import com.personal.happygallery.application.inquiry.port.out.GroupInquiryActivityPort;
import com.personal.happygallery.application.inquiry.port.out.GroupInquiryPort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.inquiry.GroupInquiry;
import com.personal.happygallery.domain.inquiry.GroupInquiryActivity;
import com.personal.happygallery.domain.inquiry.GroupInquiryDetails;
import com.personal.happygallery.domain.inquiry.GroupInquiryStatus;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalDate;
import com.personal.happygallery.domain.time.Clocks;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional
public class DefaultGroupInquiryService implements GroupInquiryUseCase {
    private final GroupInquiryPort inquiries;
    private final GroupInquiryActivityPort activities;
    private final FieldEncryptor encryptor;
    private final ObjectMapper mapper;
    private final MemberAccountGuard members;
    private final Clock clock;

    public DefaultGroupInquiryService(GroupInquiryPort inquiries, GroupInquiryActivityPort activities,
            FieldEncryptor encryptor, ObjectMapper mapper, MemberAccountGuard members, Clock clock) {
        this.inquiries = inquiries;
        this.activities = activities;
        this.encryptor = encryptor;
        this.mapper = mapper;
        this.members = members;
        this.clock = clock;
    }

    @Override
    public View create(Long userId, GroupInquiryDetails details) {
        if (userId != null && !members.requireActiveForUpdate(userId).isActive()) {
            throw new NotFoundException("회원");
        }
        return save(userId, GroupInquiry.Source.WEBSITE, details);
    }

    @Override
    public Detail createExternal(Long adminId, GroupInquiryDetails details) {
        View view = save(null, GroupInquiry.Source.EXTERNAL, details);
        activities.save(new GroupInquiryActivity(view.inquiry().getId(), adminId, null,
                GroupInquiryStatus.RECEIVED, encryptor.encrypt("외부 채널 문의 등록"), LocalDateTime.now(clock)));
        return detail(view.inquiry());
    }

    private View save(Long userId, GroupInquiry.Source source, GroupInquiryDetails details) {
        GroupInquiry inquiry = inquiries.saveAndFlush(new GroupInquiry(userId, source,
                encryptor.encrypt(mapper.writeValueAsString(details)), LocalDateTime.now(clock)));
        return new View(inquiry, details);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<View> listForAdmin(GroupInquiryStatus status, String cursor, int size) {
        return search(null, status, cursor, size);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<View> listForMember(Long userId, String cursor, int size) {
        return search(Objects.requireNonNull(userId), null, cursor, size);
    }

    private CursorPage<View> search(Long userId, GroupInquiryStatus status, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        var before = cursor == null ? null : CursorUtils.decode(cursor);
        var rows = inquiries.search(userId, status, before == null ? null : before.timestamp(),
                before == null ? null : before.id(), pageSize + 1);
        return CursorPage.of(rows.stream().map(this::view).toList(), pageSize,
                value -> CursorUtils.encode(value.inquiry().getCreatedAt(), value.inquiry().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Detail detailForAdmin(Long id) {
        return detail(inquiries.findById(id).orElseThrow(NotFoundException.supplier("단체 수업 문의")));
    }

    @Override
    public Detail update(Long id, long version, GroupInquiryStatus status, String note, Long adminId) {
        String normalizedNote = GroupInquiryDetails.optionalMessage(note);
        if (normalizedNote == null) throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "상담 메모를 입력해 주세요.");
        GroupInquiry inquiry = inquiries.findByIdForUpdate(id).orElseThrow(NotFoundException.supplier("단체 수업 문의"));
        var previous = inquiry.getStatus();
        var now = LocalDateTime.now(clock);
        inquiry.recordConsultation(version, status, now);
        inquiries.saveAndFlush(inquiry);
        activities.save(new GroupInquiryActivity(id, adminId, previous, status, encryptor.encrypt(normalizedNote), now));
        return detail(inquiry);
    }

    @Override
    public Detail scheduleContact(Long id, long version, LocalDate nextContactOn, Long adminId) {
        var inquiry = inquiries.findByIdForUpdate(id).orElseThrow(NotFoundException.supplier("단체 수업 문의"));
        var previous = inquiry.getNextContactOn();
        var now = LocalDateTime.now(clock);
        inquiry.scheduleContact(version, nextContactOn, now);
        inquiries.saveAndFlush(inquiry);
        String note = "다음 연락일: " + (previous == null ? "미지정" : previous)
                + " → " + (nextContactOn == null ? "미지정" : nextContactOn);
        activities.save(new GroupInquiryActivity(id, adminId, inquiry.getStatus(), inquiry.getStatus(), encryptor.encrypt(note), now));
        return detail(inquiry);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<View> followUps(String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        var after = cursor == null ? null : CursorUtils.decode(cursor);
        var today = LocalDate.now(clock.withZone(Clocks.SEOUL));
        var rows = inquiries.findFollowUps(today, after == null ? null : after.timestamp().toLocalDate(),
                after == null ? null : after.id(), pageSize + 1);
        return CursorPage.of(rows.stream().map(this::view).toList(), pageSize,
                value -> CursorUtils.encode(value.inquiry().getNextContactOn().atStartOfDay(), value.inquiry().getId()));
    }

    private View view(GroupInquiry inquiry) {
        return new View(inquiry, mapper.readValue(encryptor.decrypt(inquiry.getDetailsEnc()), GroupInquiryDetails.class));
    }

    private Detail detail(GroupInquiry inquiry) {
        return new Detail(view(inquiry), activities.findByInquiryIdOrderByIdDesc(inquiry.getId()).stream()
                .map(activity -> new ActivityView(activity, encryptor.decrypt(activity.getNoteEnc()))).toList());
    }
}
