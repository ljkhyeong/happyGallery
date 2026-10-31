package com.personal.happygallery.application.qna;

import com.personal.happygallery.application.customer.MemberNames;
import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.application.qna.port.out.ProductQnaListView;
import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.application.shared.page.CursorPage;
import com.personal.happygallery.application.shared.page.CursorUtils.CursorParam;
import com.personal.happygallery.application.shared.page.CursorUtils;
import com.personal.happygallery.application.shared.page.PageParams;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.error.NotFoundException;
import com.personal.happygallery.domain.notification.NotificationAggregateType;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.qna.ProductQna;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        if (!productReader.existsById(productId)) {
            throw new NotFoundException("상품");
        }
        return qnaStore.save(new ProductQna(productId, userId, title, content, secret));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<OwnedQnaListView> listOwnedByProduct(
            Long productId, Long userId, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        CursorParam after = CursorUtils.decodeOrFirst(cursor);
        List<ProductQnaListView> qnaList = qnaReader.findOwnedByProduct(
                productId, userId, after.timestamp(), after.id(), pageSize + 1);
        return CursorPage.of(
                qnaList.stream().map(DefaultProductQnaService::toOwnedView).toList(),
                pageSize,
                item -> CursorUtils.encode(item.createdAt(), item.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<PublicQnaListView> listByProduct(
            Long productId, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        CursorParam after = CursorUtils.decodeOrFirst(cursor);
        List<ProductQnaListView> qnaList = qnaReader.findByProductId(
                productId, after.timestamp(), after.id(), pageSize + 1);
        Map<Long, String> authorNames = MemberNames.of(
                userReader, qnaList.stream().map(ProductQnaListView::userId).distinct().toList());
        List<PublicQnaListView> items = qnaList.stream()
                .map(qna -> new PublicQnaListView(
                        qna.id(), qna.title(), authorNames.getOrDefault(qna.userId(), MemberNames.WITHDRAWN),
                        qna.secret(), qna.hasReply(), qna.createdAt()))
                .toList();
        return CursorPage.of(
                items,
                pageSize,
                item -> CursorUtils.encode(item.createdAt(), item.id()));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<QnaWithAuthor> listByProductForAdmin(
            Long productId, String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        CursorParam after = CursorUtils.decodeOrFirst(cursor);
        List<ProductQna> qnaList = qnaReader.findByProductIdForAdmin(
                productId, after.timestamp(), after.id(), pageSize + 1);
        return CursorPage.of(
                withAuthors(qnaList),
                pageSize,
                item -> CursorUtils.encode(item.qna().getCreatedAt(), item.qna().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorPage<QnaWithAuthor> listUnanswered(String cursor, int size) {
        int pageSize = PageParams.requireSize(size);
        CursorParam after = CursorUtils.decodeOrFirst(cursor);
        List<ProductQna> qnaList = qnaReader.findUnanswered(after.timestamp(), after.id(), pageSize + 1);
        return CursorPage.of(
                withAuthors(qnaList),
                pageSize,
                item -> CursorUtils.encode(item.qna().getCreatedAt(), item.qna().getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public QnaWithAuthor getPublicDetail(Long productId, Long qnaId) {
        ProductQna qna = qnaReader.findByIdAndProductId(qnaId, productId)
                .orElseThrow(NotFoundException.supplier("Q&A"));
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
                NotificationAggregateType.PRODUCT_QNA,
                qna.getId()));
        return withAuthor(qna);
    }

    private QnaWithAuthor withAuthor(ProductQna qna) {
        return new QnaWithAuthor(qna, MemberNames.of(userReader, qna.getUserId()));
    }

    private static OwnedQnaListView toOwnedView(ProductQnaListView qna) {
        return new OwnedQnaListView(
                qna.id(), qna.title(), qna.secret(), qna.hasReply(), qna.createdAt());
    }

    private List<QnaWithAuthor> withAuthors(List<ProductQna> qnaList) {
        Map<Long, String> authorNames = MemberNames.of(
                userReader, qnaList.stream().map(ProductQna::getUserId).distinct().toList());
        return qnaList.stream()
                .map(qna -> new QnaWithAuthor(
                        qna, authorNames.getOrDefault(qna.getUserId(), MemberNames.WITHDRAWN)))
                .toList();
    }

}
