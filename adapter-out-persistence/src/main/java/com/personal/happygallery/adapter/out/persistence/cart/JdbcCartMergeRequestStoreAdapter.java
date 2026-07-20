package com.personal.happygallery.adapter.out.persistence.cart;

import com.personal.happygallery.application.cart.port.out.CartMergeRequestStorePort;
import com.personal.happygallery.application.cart.port.out.CartMergeRequestStorePort.Registration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcCartMergeRequestStoreAdapter implements CartMergeRequestStorePort {

    private final JdbcClient jdbc;

    JdbcCartMergeRequestStoreAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Registration register(Long userId, UUID idempotencyKey, String payloadHash,
                                 LocalDateTime createdAt) {
        try {
            jdbc.sql("""
                            INSERT INTO cart_merge_requests (
                                user_id, idempotency_key, payload_hash, created_at
                            ) VALUES (
                                :userId, :idempotencyKey, :payloadHash, :createdAt
                            )
                            """)
                    .param("userId", userId)
                    .param("idempotencyKey", idempotencyKey.toString())
                    .param("payloadHash", payloadHash)
                    .param("createdAt", createdAt)
                    .update();
            return Registration.RECORDED;
        } catch (DuplicateKeyException e) {
            String recordedHash = jdbc.sql("""
                            SELECT payload_hash
                            FROM cart_merge_requests
                            WHERE user_id = :userId
                              AND idempotency_key = :idempotencyKey
                            """)
                    .param("userId", userId)
                    .param("idempotencyKey", idempotencyKey.toString())
                    .query(String.class)
                    .single();
            return payloadHash.equals(recordedHash)
                    ? Registration.REPLAY
                    : Registration.CONFLICT;
        }
    }
}
