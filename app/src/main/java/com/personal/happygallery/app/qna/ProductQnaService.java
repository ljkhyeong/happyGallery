package com.personal.happygallery.app.qna;

import com.personal.happygallery.app.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.app.customer.port.out.UserReaderPort;
import com.personal.happygallery.app.product.port.out.ProductReaderPort;
import com.personal.happygallery.app.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.app.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.common.error.HappyGalleryException;
import com.personal.happygallery.common.error.ErrorCode;
import com.personal.happygallery.common.error.NotFoundException;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQnaService implements ProductQnaUseCase {

    private final ProductQnaReaderPort qnaReader;
    private final ProductQnaStorePort qnaStore;
    private final ProductReaderPort productReader;
    private final UserReaderPort userReader;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder;

    public ProductQnaService(ProductQnaReaderPort qnaReader,
                             ProductQnaStorePort qnaStore,
                             ProductReaderPort productReader,
                             UserReaderPort userReader,
                             Clock clock,
                             PasswordEncoder passwordEncoder) {
        this.qnaReader = qnaReader;
        this.qnaStore = qnaStore;
        this.productReader = productReader;
        this.userReader = userReader;
        this.clock = clock;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public ProductQna createQuestion(Long productId, Long userId, String title, String content,
                                     boolean secret, String rawPassword) {
        productReader.findById(productId)
                .orElseThrow(() -> new NotFoundException("상품"));
        String hash = (secret && rawPassword != null) ? passwordEncoder.encode(rawPassword) : null;
        return qnaStore.save(new ProductQna(productId, userId, title, content, secret, hash));
    }

    @Transactional(readOnly = true)
    public List<QnaWithAuthor> listByProduct(Long productId) {
        List<ProductQna> qnaList = qnaReader.findByProductId(productId);
        Map<Long, User> userMap = batchFetchUsers(qnaList);
        return qnaList.stream()
                .map(q -> new QnaWithAuthor(q, userName(userMap, q.getUserId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public QnaWithAuthor verifyAndGet(Long qnaId, String rawPassword) {
        ProductQna qna = qnaReader.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A"));
        if (qna.isSecret()) {
            if (rawPassword == null || !passwordEncoder.matches(rawPassword, qna.getPasswordHash())) {
                throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "비밀번호가 일치하지 않습니다.");
            }
        }
        String authorName = userReader.findById(qna.getUserId())
                .map(User::getName).orElse("탈퇴회원");
        return new QnaWithAuthor(qna, authorName);
    }

    @Transactional(readOnly = true)
    public List<QnaWithAuthor> listAll() {
        List<ProductQna> qnaList = qnaReader.findByProductId(null);
        // admin은 전체 조회 — adapter에서 null 처리 불가하므로 별도 메서드 필요할 수 있으나
        // 현재는 productId별 조회만 사용하고 admin은 컨트롤러에서 분기
        Map<Long, User> userMap = batchFetchUsers(qnaList);
        return qnaList.stream()
                .map(q -> new QnaWithAuthor(q, userName(userMap, q.getUserId())))
                .toList();
    }

    @Transactional
    public ProductQna reply(Long qnaId, String replyContent, Long adminId) {
        ProductQna qna = qnaReader.findById(qnaId)
                .orElseThrow(() -> new NotFoundException("Q&A"));
        qna.reply(replyContent, adminId, LocalDateTime.now(clock));
        return qnaStore.save(qna);
    }

    private Map<Long, User> batchFetchUsers(List<ProductQna> qnaList) {
        List<Long> userIds = qnaList.stream().map(ProductQna::getUserId).distinct().toList();
        return userReader.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private static String userName(Map<Long, User> userMap, Long userId) {
        User user = userMap.get(userId);
        return user != null ? user.getName() : "탈퇴회원";
    }

    public record QnaWithAuthor(ProductQna qna, String authorName) {}
}
