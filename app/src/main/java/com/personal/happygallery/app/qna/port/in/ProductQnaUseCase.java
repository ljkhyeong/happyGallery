package com.personal.happygallery.app.qna.port.in;

import com.personal.happygallery.app.qna.ProductQnaService.QnaWithAuthor;
import com.personal.happygallery.domain.qna.ProductQna;
import java.util.List;

/**
 * 상품 Q&A 유스케이스.
 *
 * <p>고객 질문 등록·비밀글 열람과 운영자 답변을 지원한다.
 */
public interface ProductQnaUseCase {

    ProductQna createQuestion(Long productId, Long userId, String title, String content,
                              boolean secret, String rawPassword);

    List<QnaWithAuthor> listByProduct(Long productId);

    QnaWithAuthor verifyAndGet(Long qnaId, String rawPassword);

    ProductQna reply(Long qnaId, String replyContent, Long adminId);
}
