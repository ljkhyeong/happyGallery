package com.personal.happygallery.app.inquiry.port.in;

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

    Inquiry findByIdAndUser(Long inquiryId, Long userId);

    List<InquiryWithUser> listAll();

    InquiryWithUser findByIdForAdmin(Long inquiryId);

    Inquiry reply(Long inquiryId, String replyContent, Long adminId);
}
