package com.personal.happygallery.application.qna;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.customer.port.out.UserReaderPort;
import com.personal.happygallery.application.product.port.out.ProductReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaReaderPort;
import com.personal.happygallery.application.qna.port.out.ProductQnaStorePort;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import com.personal.happygallery.domain.qna.ProductQna;
import com.personal.happygallery.domain.user.User;
import java.time.Clock;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

@Tag("policy")
class ProductQnaVisibilityPolicyTest {

    @Test
    @DisplayName("일반 Q&A는 공개 상세로 조회하고 비밀 Q&A는 비밀번호 없는 조회를 차단한다")
    void publicDetailDistinguishesPublicAndSecretQuestions() {
        ProductQnaReaderPort reader = mock(ProductQnaReaderPort.class);
        UserReaderPort userReader = mock(UserReaderPort.class);
        ProductQna publicQna = mock(ProductQna.class);
        ProductQna secretQna = mock(ProductQna.class);
        User author = mock(User.class);

        when(reader.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(publicQna));
        when(reader.findByIdAndProductId(20L, 1L)).thenReturn(Optional.of(secretQna));
        when(publicQna.getUserId()).thenReturn(3L);
        when(secretQna.isSecret()).thenReturn(true);
        when(userReader.findById(3L)).thenReturn(Optional.of(author));
        when(author.getName()).thenReturn("홍길동");

        DefaultProductQnaService service = new DefaultProductQnaService(
                reader,
                mock(ProductQnaStorePort.class),
                mock(ProductReaderPort.class),
                userReader,
                Clock.systemUTC(),
                mock(PasswordEncoder.class),
                mock(ApplicationEventPublisher.class));

        assertThat(service.getPublicDetail(1L, 10L).authorName()).isEqualTo("홍길동");
        assertThatThrownBy(() -> service.getPublicDetail(1L, 20L))
                .isInstanceOfSatisfying(
                        HappyGalleryException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
    }
}
