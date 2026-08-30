package com.personal.happygallery.application.notification;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.inquiry.DefaultInquiryService;
import com.personal.happygallery.application.inquiry.port.out.InquiryReaderPort;
import com.personal.happygallery.application.inquiry.port.out.InquiryStorePort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.qna.DefaultProductQnaService;
import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.domain.inquiry.Inquiry;
import com.personal.happygallery.domain.notification.NotificationEventType;
import com.personal.happygallery.domain.notification.NotificationRequestedEvent;
import com.personal.happygallery.domain.qna.ProductQna;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AnswerNotificationRequestTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-07-21T00:00:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("문의 답변은 회원 알림을 문의별 멱등키로 요청한다")
    void inquiryReplyRequestsMemberNotification() {
        InquiryReaderPort reader = mock(InquiryReaderPort.class);
        InquiryStorePort store = mock(InquiryStorePort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        Inquiry inquiry = mock(Inquiry.class);
        when(reader.findByIdForUpdate(20L)).thenReturn(Optional.of(inquiry));
        when(store.save(inquiry)).thenReturn(inquiry);
        when(inquiry.getUserId()).thenReturn(10L);
        when(inquiry.getId()).thenReturn(20L);
        when(userReader.findById(10L)).thenReturn(Optional.empty());
        var service = new DefaultInquiryService(reader, store, userReader, publisher, CLOCK);

        service.replyAndGet(20L, "답변", 1L);

        NotificationRequestedEvent.ForUser event = capturedEvent(publisher);
        assertThat(event.eventType()).isEqualTo(NotificationEventType.INQUIRY_ANSWERED);
        assertThat(event.idempotencyKey()).isEqualTo("USER:10:INQUIRY_ANSWERED:INQUIRY:20");
    }

    @Test
    @DisplayName("상품 Q&A 답변은 회원 알림을 게시글별 멱등키로 요청한다")
    void productQnaReplyRequestsMemberNotification() {
        ProductQnaReaderPort reader = mock(ProductQnaReaderPort.class);
        ProductQnaStorePort store = mock(ProductQnaStorePort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ProductQna qna = mock(ProductQna.class);
        when(reader.findByIdForUpdate(30L)).thenReturn(Optional.of(qna));
        when(store.save(qna)).thenReturn(qna);
        when(qna.getUserId()).thenReturn(10L);
        when(qna.getId()).thenReturn(30L);
        when(userReader.findById(10L)).thenReturn(Optional.empty());
        var service = new DefaultProductQnaService(
                reader,
                store,
                mock(ProductReaderPort.class),
                userReader,
                CLOCK,
                publisher);

        service.replyAndGet(30L, "답변", 1L);

        NotificationRequestedEvent.ForUser event = capturedEvent(publisher);
        assertThat(event.eventType()).isEqualTo(NotificationEventType.PRODUCT_QNA_ANSWERED);
        assertThat(event.idempotencyKey()).isEqualTo(
                "USER:10:PRODUCT_QNA_ANSWERED:PRODUCT_QNA:30");
    }

    private static NotificationRequestedEvent.ForUser capturedEvent(
            ApplicationEventPublisher publisher) {
        ArgumentCaptor<NotificationRequestedEvent.ForUser> captor =
                ArgumentCaptor.forClass(NotificationRequestedEvent.ForUser.class);
        verify(publisher).publishEvent(captor.capture());
        return captor.getValue();
    }
}
