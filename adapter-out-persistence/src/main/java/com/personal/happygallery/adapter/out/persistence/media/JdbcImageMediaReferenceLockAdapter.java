package com.personal.happygallery.adapter.out.persistence.media;

import com.personal.happygallery.application.media.port.out.ImageMediaReferenceLockPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcImageMediaReferenceLockAdapter implements ImageMediaReferenceLockPort {

    private static final String LOCK_SQL =
            "SELECT id FROM image_media_reference_lock WHERE id = 1 FOR UPDATE";

    private final JdbcTemplate jdbcTemplate;

    public JdbcImageMediaReferenceLockAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock() {
        Integer lockId = jdbcTemplate.queryForObject(LOCK_SQL, Integer.class);
        if (lockId == null) {
            throw new IllegalStateException("이미지 참조 잠금 행이 없습니다.");
        }
    }
}
