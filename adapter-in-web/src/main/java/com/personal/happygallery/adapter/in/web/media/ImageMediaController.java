package com.personal.happygallery.adapter.in.web.media;

import com.personal.happygallery.application.media.port.in.ImageMediaUseCase;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/media/images")
public class ImageMediaController {

    private final ImageMediaUseCase imageMediaUseCase;

    public ImageMediaController(ImageMediaUseCase imageMediaUseCase) {
        this.imageMediaUseCase = imageMediaUseCase;
    }

    @GetMapping("/{fileName}")
    public ResponseEntity<byte[]> get(@PathVariable String fileName) {
        ImageMediaUseCase.ImageContent image = imageMediaUseCase.get(fileName);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
                .body(image.bytes());
    }
}
