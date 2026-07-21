package com.personal.happygallery.application.media.port.out;

import java.util.Optional;

public interface ImageMediaStoragePort {

    void store(String fileName, byte[] bytes);

    Optional<byte[]> read(String fileName);
}
