package com.personal.happygallery.adapter.out.persistence.media;

import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcImageMediaReferenceReaderAdapter implements ImageMediaReferenceReaderPort {

    private final JdbcClient jdbc;

    JdbcImageMediaReferenceReaderAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<String> findReferencedImageUrls() {
        return jdbc.sql("""
                        SELECT image_url
                        FROM products
                        WHERE image_url IS NOT NULL
                        UNION
                        SELECT image_url
                        FROM classes
                        WHERE image_url IS NOT NULL
                        UNION
                        SELECT image_url
                        FROM events
                        WHERE image_url IS NOT NULL
                        UNION
                        SELECT image_url
                        FROM review_images
                        """)
                .query(String.class)
                .list();
    }
}
