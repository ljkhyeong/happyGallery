package com.personal.happygallery.application.customer;

import com.personal.happygallery.adapter.out.persistence.inquiry.InquiryRepository;
import com.personal.happygallery.adapter.out.persistence.product.ProductRepository;
import com.personal.happygallery.adapter.out.persistence.qna.ProductQnaRepository;
import com.personal.happygallery.application.customer.port.out.UserStorePort;
import com.personal.happygallery.application.inquiry.port.in.InquiryUseCase;
import com.personal.happygallery.application.qna.port.in.ProductQnaUseCase;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.product.Product;
import com.personal.happygallery.domain.product.ProductType;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.User;
import com.personal.happygallery.support.TestCleanupSupport;
import com.personal.happygallery.support.UseCaseIT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@UseCaseIT
class CustomerHistoryPaginationUseCaseIT {

    @Autowired ProductQnaUseCase productQnaUseCase;
    @Autowired InquiryUseCase inquiryUseCase;
    @Autowired UserStorePort userStore;
    @Autowired ProductRepository productRepository;
    @Autowired ProductQnaRepository productQnaRepository;
    @Autowired InquiryRepository inquiryRepository;
    @Autowired TestCleanupSupport cleanupSupport;

    @AfterEach
    void tearDown() {
        productQnaRepository.deleteAllInBatch();
        inquiryRepository.deleteAllInBatch();
        cleanupSupport.clearProductData();
        cleanupSupport.clearUsers();
    }

    @Test
    @DisplayName("상품 QNA 공개·작성자 목록은 본문 없이 커서로 다음 항목을 이어 조회한다")
    void productQnaLists_continueWithCursor() {
        User user = createUser();
        Product product = productRepository.save(
                new Product("커서 상품", ProductType.READY_STOCK, 30_000L));
        ProductQna first = productQnaRepository.save(new ProductQna(
                product.getId(), user.getId(), "첫 질문", "첫 질문 본문", false));
        ProductQna second = productQnaRepository.save(new ProductQna(
                product.getId(), user.getId(), "둘째 질문", "둘째 질문 본문", true));

        var publicFirstPage = productQnaUseCase.listByProduct(product.getId(), null, 1);
        var publicSecondPage = productQnaUseCase.listByProduct(
                product.getId(), publicFirstPage.nextCursor(), 1);
        var ownedFirstPage = productQnaUseCase.listOwnedByProduct(
                product.getId(), user.getId(), null, 1);
        var ownedSecondPage = productQnaUseCase.listOwnedByProduct(
                product.getId(), user.getId(), ownedFirstPage.nextCursor(), 1);

        assertSoftly(softly -> {
            softly.assertThat(publicFirstPage.content())
                    .extracting(ProductQnaUseCase.PublicQnaListView::id)
                    .containsExactly(second.getId());
            softly.assertThat(publicFirstPage.hasMore()).isTrue();
            softly.assertThat(publicSecondPage.content())
                    .extracting(ProductQnaUseCase.PublicQnaListView::id)
                    .containsExactly(first.getId());
            softly.assertThat(publicSecondPage.hasMore()).isFalse();
            softly.assertThat(ownedFirstPage.content())
                    .extracting(ProductQnaUseCase.OwnedQnaListView::id)
                    .containsExactly(second.getId());
            softly.assertThat(ownedSecondPage.content())
                    .extracting(ProductQnaUseCase.OwnedQnaListView::id)
                    .containsExactly(first.getId());
        });
    }

    @Test
    @DisplayName("회원 문의 목록은 같은 시각의 항목도 식별자 커서로 빠짐없이 이어 조회한다")
    void inquiryList_continuesWithCursor() {
        User user = createUser();
        Inquiry first = inquiryRepository.save(
                new Inquiry(user.getId(), "첫 문의", "첫 문의 본문"));
        Inquiry second = inquiryRepository.save(
                new Inquiry(user.getId(), "둘째 문의", "둘째 문의 본문"));

        var firstPage = inquiryUseCase.listByUser(user.getId(), null, 1);
        var secondPage = inquiryUseCase.listByUser(
                user.getId(), firstPage.nextCursor(), 1);

        assertThat(firstPage.content()).extracting(Inquiry::getId)
                .containsExactly(second.getId());
        assertThat(firstPage.hasMore()).isTrue();
        assertThat(secondPage.content()).extracting(Inquiry::getId)
                .containsExactly(first.getId());
        assertThat(secondPage.hasMore()).isFalse();
    }

    private User createUser() {
        return userStore.save(new User(
                "history-page@example.com", "password-hash", "이력 회원", "01012345678"));
    }
}
