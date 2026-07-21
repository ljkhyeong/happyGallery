package com.personal.happygallery.adapter.in.web.admin;

import com.personal.happygallery.adapter.in.web.admin.dto.ImageUploadResponse;
import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import com.personal.happygallery.domain.error.ErrorCode;
import com.personal.happygallery.domain.error.HappyGalleryException;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/media")
public class AdminMediaController {

    private final ImageMediaUseCase imageMediaUseCase;

    public AdminMediaController(ImageMediaUseCase imageMediaUseCase) {
        this.imageMediaUseCase = imageMediaUseCase;
    }

    @PostMapping(path = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImageUploadResponse uploadImage(@RequestPart("file") MultipartFile file) {
        try {
            return ImageUploadResponse.from(
                    imageMediaUseCase.upload(file.getBytes(), file.getContentType()));
        } catch (IOException e) {
            throw new HappyGalleryException(ErrorCode.INVALID_INPUT, "이미지 파일을 읽을 수 없습니다.");
        }
    }
}
