package com.personal.happygallery.application.qna;

import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static java.util.stream.Collectors.toMap;

@Service
public class DefaultProductQnaService implements ProductQnaUseCase {

    private final ProductQnaReaderPort qnaReader;
    private final ProductQnaStorePort qnaStore;
    private final ProductReaderPort productReader;
    private final UserReaderPort userReader;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultProductQnaService(ProductQnaReaderPort qnaReader,
                                    ProductQnaStorePort qnaStore,
                                    ProductReaderPort productReader,
                                    UserReaderPort userReader,
                                    Clock clock,
                                    ApplicationEventPublisher eventPublisher) {
        this.qnaReader = qnaReader;
        this.qnaStore = qnaStore;
        this.productReader = productReader;
        this.userReader = userReader;
        this.clock = clock;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ProductQna createQuestion(Long productId, Long userId, String title, String content,
                                     boolean secret) {
        productReader.findById(productId)
                .orElseThrow(NotFoundException.supplier("상품"));
        return qnaStore.save(new ProductQna(productId, userId, title, content, secret));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductQna> listOwnedByProduct(Long productId, Long userId) {
        return qnaReader.findOwnedByProduct(productId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QnaWithAuthor> listByProduct(Long productId) {
        List<ProductQna> qnaList = qnaReader.findByProductId(productId);
        return withAuthors(qnaList);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<QnaWithAuthor> listUnanswered(String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        int fetchSize = pageSize + 1;
        List<ProductQna> qnaList;
        if (cursor == null) {
            qnaList = qnaReader.findUnanswered(fetchSize);
        } else {
            var cursorParam = CursorUtils.decode(cursor);
            qnaList = qnaReader.findUnansweredAfter(
                    cursorParam.timestamp(), cursorParam.id(), fetchSize);
        }
        return CursorPage.of(
                withAuthors(qnaList),
                pageSize,
                item -> CursorUtils.encode(item.qna().getCreatedAt(), item.qna().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public QnaWithAuthor getPublicDetail(Long productId, Long qnaId) {
        ProductQna qna = findByProduct(productId, qnaId);
        if (qna.isSecret()) {
            throw new HappyGalleryException(
                    ErrorCode.FORBIDDEN,
                    "비밀글은 작성자만 조회할 수 있습니다.");
        }
        return withAuthor(qna);
    }

    @Override
    @Transactional(readOnly = true)
    public QnaWithAuthor getOwnedDetail(Long productId, Long qnaId, Long userId) {
        ProductQna qna = qnaReader.findByIdAndProductIdAndUserId(qnaId, productId, userId)
                .orElseThrow(NotFoundException.supplier("Q&A"));
        return withAuthor(qna);
    }

    @Override
    @Transactional
    public QnaWithAuthor replyAndGet(Long qnaId, String replyContent, Long adminId) {
        ProductQna qna = qnaReader.findByIdForUpdate(qnaId)
                .orElseThrow(NotFoundException.supplier("Q&A"));
        qna.reply(replyContent, adminId, LocalDateTime.now(clock));
        qna = qnaStore.save(qna);
        eventPublisher.publishEvent(NotificationRequestedEvent.forUser(
                qna.getUserId(),
                NotificationEventType.PRODUCT_QNA_ANSWERED,
                "PRODUCT_QNA",
                qna.getId()));
        String authorName = userReader.findById(qna.getUserId())
                .map(User::getName).orElse("탈퇴회원");
        return new QnaWithAuthor(qna, authorName);
    }

    private ProductQna findByProduct(Long productId, Long qnaId) {
        return qnaReader.findByIdAndProductId(qnaId, productId)
                .orElseThrow(NotFoundException.supplier("Q&A"));
    }

    private QnaWithAuthor withAuthor(ProductQna qna) {
        String authorName = userReader.findById(qna.getUserId())
                .map(User::getName).orElse("탈퇴회원");
        return new QnaWithAuthor(qna, authorName);
    }

    private Map<Long, User> batchFetchUsers(List<ProductQna> qnaList) {
        List<Long> userIds = qnaList.stream().map(ProductQna::getUserId).distinct().toList();
        return userReader.findAllById(userIds).stream()
                .collect(toMap(User::getId, Function.identity()));
    }

    private List<QnaWithAuthor> withAuthors(List<ProductQna> qnaList) {
        Map<Long, User> userMap = batchFetchUsers(qnaList);
        return qnaList.stream()
                .map(qna -> new QnaWithAuthor(qna, userName(userMap, qna.getUserId())))
                .toList();
    }

    private static String userName(Map<Long, User> userMap, Long userId) {
        User user = userMap.get(userId);
        return user != null ? user.getName() : "탈퇴회원";
    }

}
