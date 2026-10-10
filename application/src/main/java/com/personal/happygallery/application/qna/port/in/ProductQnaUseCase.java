package com.personal.happygallery.application.qna.port.in;

import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.domain.qna.ProductQna;
import java.time.LocalDateTime;

/**
 * 상품 Q&A 유스케이스.
 *
 * <p>고객 질문 등록·작성자 전용 비밀글 열람과 운영자 답변을 지원한다.
 */
public interface ProductQnaUseCase {

    record QnaWithAuthor(ProductQna qna, String authorName) {}

    record PublicQnaListView(Long id,
                             String title,
                             String authorName,
                             boolean secret,
                             boolean hasReply,
                             LocalDateTime createdAt) {}

    record OwnedQnaListView(Long id,
                            String title,
                            boolean secret,
                            boolean hasReply,
                            LocalDateTime createdAt) {}

    ProductQna createQuestion(Long productId, Long userId, String title, String content,
                              boolean secret);

    CursorPage<OwnedQnaListView> listOwnedByProduct(
            Long productId, Long userId, String cursor, int size);

    CursorPage<PublicQnaListView> listByProduct(Long productId, String cursor, int size);

    CursorPage<QnaWithAuthor> listByProductForAdmin(
            Long productId, String cursor, int size);

    CursorPage<QnaWithAuthor> listUnanswered(String cursor, int size);

    QnaWithAuthor getPublicDetail(Long productId, Long qnaId);

    QnaWithAuthor getOwnedDetail(Long productId, Long qnaId, Long userId);

    QnaWithAuthor replyAndGet(Long qnaId, String replyContent, Long adminId);
}
