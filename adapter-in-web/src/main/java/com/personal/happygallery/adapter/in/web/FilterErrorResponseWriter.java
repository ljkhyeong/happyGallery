package com.personal.happygallery.adapter.in.web;

import com.personal.happygallery.adapter.in.web.error.ErrorResponse;
import com.personal.happygallery.domain.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import tools.jackson.databind.ObjectMapper;

public final class FilterErrorResponseWriter {

    private FilterErrorResponseWriter() {}

    public static void write(HttpServletResponse response, ObjectMapper mapper,
                             ErrorCode code) throws IOException {
        write(response, mapper, code, code.message);
    }

    public static void write(HttpServletResponse response, ObjectMapper mapper,
                             ErrorCode code, String message) throws IOException {
        response.setStatus(code.httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(
                StreamUtils.nonClosing(response.getOutputStream()),
                ErrorResponse.of(code, message, MDC.get("requestId")));
    }
}
