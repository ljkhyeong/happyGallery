package com.personal.happygallery.adapter.out.persistence.media;

import com.personal.happygallery.application.media.port.out.ImageMediaReferenceReaderPort;
import java.time.LocalDateTime;
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
                        UNION
                        SELECT image_url
                        FROM review_evidence_snapshot_images
                        """)
                .query(String.class)
                .list();
    }

    @Override
    public boolean isPubliclyReferenced(String imageUrl, LocalDateTime now) {
        return jdbc.sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM products
                            WHERE (image_url = :imageUrl
                                OR image_url LIKE CONCAT(:imageUrl, '?%')
                                OR image_url LIKE CONCAT(:imageUrl, '#%'))
                              AND status = 'ACTIVE'
                            UNION ALL
                            SELECT 1
                            FROM classes
                            WHERE (image_url = :imageUrl
                                OR image_url LIKE CONCAT(:imageUrl, '?%')
                                OR image_url LIKE CONCAT(:imageUrl, '#%'))
                              AND status = 'ACTIVE'
                            UNION ALL
                            SELECT 1
                            FROM events
                            WHERE (image_url = :imageUrl
                                OR image_url LIKE CONCAT(:imageUrl, '?%')
                                OR image_url LIKE CONCAT(:imageUrl, '#%'))
                              AND published = TRUE
                              AND end_at > :now
                            UNION ALL
                            SELECT 1
                            FROM review_images image
                            JOIN reviews review ON review.id = image.review_id
                            WHERE (image.image_url = :imageUrl
                                OR image.image_url LIKE CONCAT(:imageUrl, '?%')
                                OR image.image_url LIKE CONCAT(:imageUrl, '#%'))
                              AND review.status = 'PUBLISHED'
                              AND review.deleted_at IS NULL
                        )
                        """)
                .param("imageUrl", imageUrl)
                .param("now", now)
                .query(Boolean.class)
                .single();
    }
}
