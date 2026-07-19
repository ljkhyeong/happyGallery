package com.personal.happygallery.adapter.out.persistence.admin;

import com.personal.happygallery.application.admin.port.out.AdminSetupLockPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class JdbcAdminSetupLockAdapter implements AdminSetupLockPort {

    private static final String LOCK_SQL =
            "SELECT id FROM admin_setup_lock WHERE id = 1 FOR UPDATE";

    private final JdbcTemplate jdbcTemplate;

    public JdbcAdminSetupLockAdapter(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lock() {
        Integer lockId = jdbcTemplate.queryForObject(LOCK_SQL, Integer.class);
        if (lockId == null) {
            throw new IllegalStateException("관리자 setup 잠금 행이 없습니다.");
        }
    }
}
