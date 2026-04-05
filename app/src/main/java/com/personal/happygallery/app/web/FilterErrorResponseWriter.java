package com.personal.happygallery.app.web;

import com.personal.happygallery.app.web.error.ErrorResponse;
import com.personal.happygallery.domain.error.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                mapper.writeValueAsString(ErrorResponse.of(code, message)));
    }
}
