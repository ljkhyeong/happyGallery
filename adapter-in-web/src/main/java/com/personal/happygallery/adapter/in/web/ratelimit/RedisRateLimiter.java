package com.personal.happygallery.adapter.in.web.ratelimit;

import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties;
import com.personal.happygallery.adapter.in.web.config.properties.RateLimitProperties.Rule;
import com.personal.happygallery.domain.crypto.BlindIndexer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RedisRateLimiter.class);

    private static final RedisScript<Long> INCREMENT_SCRIPT = RedisScript.of("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
                redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final BlindIndexer blindIndexer;
    private final RateLimitProperties properties;
    private final AtomicBoolean backendUnavailable = new AtomicBoolean();

    public RedisRateLimiter(StringRedisTemplate redisTemplate,
                            BlindIndexer blindIndexer,
                            RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.blindIndexer = blindIndexer;
        this.properties = properties;
    }

    /** Redis 장애 정책은 호출 경로가 결정할 수 있도록 카운터 사용 불가를 빈 결과로 반환한다. */
    public Optional<RateLimitDecision> tryConsume(String ruleId, String subject, Rule rule) {
        String key = properties.keyPrefix() + ":" + ruleId + ":" + blindIndexer.index(subject);

        try {
            Long count = redisTemplate.execute(
                    INCREMENT_SCRIPT, List.of(key), String.valueOf(rule.window().toSeconds()));
            if (count == null) {
                return unavailable(ruleId, "NO_RESULT");
            }

            markAvailable();
            long remaining = Math.max(0, rule.capacity() - count);
            return Optional.of(new RateLimitDecision(
                    rule.capacity(), remaining, rule.window(), count > rule.capacity()));
        } catch (DataAccessException e) {
            return unavailable(ruleId, e.getClass().getSimpleName());
        }
    }

    private Optional<RateLimitDecision> unavailable(String ruleId, String type) {
        if (backendUnavailable.compareAndSet(false, true)) {
            log.warn("rate limit Redis unavailable [rule={}, type={}]", ruleId, type);
        }
        return Optional.empty();
    }

    private void markAvailable() {
        if (backendUnavailable.compareAndSet(true, false)) {
            log.info("rate limit Redis recovered");
        }
    }
}
