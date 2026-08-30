package com.personal.happygallery.adapter.out.persistence.cart;

import com.personal.happygallery.application.cart.port.out.CartOwnerLockPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCartOwnerLockAdapter implements CartOwnerLockPort {

    private final JdbcClient jdbc;

    JdbcCartOwnerLockAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void lock(Long userId) {
        jdbc.sql("SELECT id FROM users WHERE id = :userId FOR UPDATE")
                .param("userId", userId)
                .query(Long.class)
                .single();
    }
}
