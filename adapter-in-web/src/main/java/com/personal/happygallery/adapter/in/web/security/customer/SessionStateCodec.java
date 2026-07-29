package com.personal.happygallery.adapter.in.web.security.customer;

import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** Redis 세션을 특정 애플리케이션 클래스에 결합하지 않도록 상태를 JSON 문자열로 저장한다. */
@Component
public final class SessionStateCodec {

    private final ObjectMapper objectMapper;

    public SessionStateCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(Object state) {
        return objectMapper.writeValueAsString(state);
    }

    public <T> T decode(Object storedState, Class<T> stateType) {
        if (!(storedState instanceof String json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, stateType);
        } catch (JacksonException exception) {
            return null;
        }
    }
}
