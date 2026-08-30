package com.personal.happygallery.adapter.in.web.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.application.media.port.in.PublicImageMediaUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ImageMediaControllerTest {

    @Test
    @DisplayName("공개 이미지 응답은 노출 철회 뒤 브라우저 캐시가 남지 않도록 no-store를 사용한다")
    void respondWithNoStoreCacheControl() {
        PublicImageMediaUseCase media = mock(PublicImageMediaUseCase.class);
        String fileName = "11111111-1111-1111-1111-111111111111.jpg";
        when(media.get(fileName)).thenReturn(new ImageMediaUseCase.ImageContent(
                new byte[] {1, 2, 3}, "image/jpeg"));
        ImageMediaController controller = new ImageMediaController(media);

        var response = controller.get(fileName);

        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).containsExactly(1, 2, 3);
    }
}
