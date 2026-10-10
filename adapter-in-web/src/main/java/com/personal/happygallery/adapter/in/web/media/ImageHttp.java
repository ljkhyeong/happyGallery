package com.personal.happygallery.adapter.in.web.media;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase.ImageContent;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.io.IOException;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

/** 이미지 업로드 본문 읽기와 이미지 바이트 응답을 컨트롤러마다 같은 방식으로 처리한다. */
public final class ImageHttp {

    private ImageHttp() {}

    /** 저장된 이미지 형식 그대로, 브라우저·프록시에 캐시하지 않는 응답으로 내려준다. */
    public static ResponseEntity<byte[]> noStore(ImageContent image) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.noStore())
                .body(image.bytes());
    }

    /** 업로드한 파일을 읽지 못하면 입력 오류로 응답한다. */
    public static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미지 파일을 읽을 수 없습니다.");
        }
    }
}
