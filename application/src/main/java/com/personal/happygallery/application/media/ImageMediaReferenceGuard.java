package com.personal.happygallery.application.media;

import com.personal.happygallery.application.media.port.out.ImageMediaReferenceLockPort;
import com.personal.happygallery.application.media.port.out.ImageMediaStoragePort;
import com.personal.happygallery.domain.error.NotFoundException;
import java.net.URI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ImageMediaReferenceGuard {

    private static final String IMAGE_PATH_PREFIX = "/api/v1/media/images/";

    private final ImageMediaReferenceLockPort lockPort;
    private final ImageMediaStoragePort storagePort;

    public ImageMediaReferenceGuard(ImageMediaReferenceLockPort lockPort,
                                    ImageMediaStoragePort storagePort) {
        this.lockPort = lockPort;
        this.storagePort = storagePort;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void validateAssignment(String imageUrl) {
        String fileName = localFileName(imageUrl);
        if (fileName == null) {
            return;
        }

        lockPort.lock();
        if (!storagePort.exists(fileName)) {
            throw new NotFoundException("이미지");
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void lockForRetention() {
        lockPort.lock();
    }

    static String localFileName(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return null;
        }
        URI uri;
        try {
            uri = URI.create(imageUrl);
        } catch (IllegalArgumentException exception) {
            return null;
        }
        if (uri.isAbsolute() || uri.getRawAuthority() != null) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || !path.startsWith(IMAGE_PATH_PREFIX)) {
            return null;
        }
        String fileName = path.substring(IMAGE_PATH_PREFIX.length());
        return fileName.indexOf('/') < 0 ? fileName : null;
    }
}
