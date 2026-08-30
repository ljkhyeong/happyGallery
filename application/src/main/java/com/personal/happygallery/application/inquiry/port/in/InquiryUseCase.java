package com.personal.happygallery.application.inquiry.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.inquiry.Inquiry;
import java.util.List;

/**
 * 회원 문의 유스케이스.
 *
 * <p>회원 문의 등록·조회와 운영자 답변을 지원한다.
 */
public interface InquiryUseCase {

    record InquiryWithUser(Inquiry inquiry, String userName) {}

    Inquiry create(Long userId, String title, String content);

    List<Inquiry> listByUser(Long userId);

    CursorPage<Inquiry> listByUser(Long userId, String cursor, int size);

    Inquiry findByIdAndUser(Long inquiryId, Long userId);

    CursorPage<InquiryWithUser> listAll(String cursor, int size);

    InquiryWithUser findByIdForAdmin(Long inquiryId);

    InquiryWithUser replyAndGet(Long inquiryId, String replyContent, Long adminId);
}
