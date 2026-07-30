package com.personal.happygallery.bootstrap.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;
import com.fasterxml.jackson.core.JsonFactory;
import java.io.StringWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MaskingPatternLayoutTest {

    private static final String SIGNED_ACCESS_TOKEN =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY6MTc3NzU5MzYwMA."
                    + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ";

    @DisplayName("실제 Base64URL 서명 접근 토큰은 헤더 구분자와 JSON 따옴표를 보존해 마스킹한다")
    @Test
    void maskSensitive_signedAccessToken_preservesLogStructure() {
        String message = """
                headers={"X-Access-Token":"%s"} X-Access-Token: %s trace.id=value.with.dot
                """.formatted(SIGNED_ACCESS_TOKEN, SIGNED_ACCESS_TOKEN);

        String masked = MaskingPatternLayout.maskSensitive(message);

        assertThat(masked)
                .doesNotContain(SIGNED_ACCESS_TOKEN)
                .contains("\"X-Access-Token\":\"***\"")
                .contains("X-Access-Token: ***")
                .contains("trace.id=value.with.dot");
    }

    @DisplayName("운영 JSON stack trace는 throwable 메시지의 접근 토큰을 마스킹한다")
    @Test
    void stackTraceProvider_masksThrowableMessage() throws Exception {
        LoggerContext context = new LoggerContext();
        LoggingEvent event = new LoggingEvent();
        event.setThrowableProxy(new ThrowableProxy(
                new IllegalStateException("invalid X-Access-Token=" + SIGNED_ACCESS_TOKEN)));
        MaskingStackTraceJsonProvider provider = new MaskingStackTraceJsonProvider();
        provider.setContext(context);
        provider.start();

        StringWriter output = new StringWriter();
        try (var generator = new JsonFactory().createGenerator(output)) {
            generator.writeStartObject();
            provider.writeTo(generator, event);
            generator.writeEndObject();
        } finally {
            provider.stop();
            context.stop();
        }

        assertThat(output.toString())
                .doesNotContain(SIGNED_ACCESS_TOKEN)
                .contains("X-Access-Token=***")
                .contains("IllegalStateException")
                .contains("stack_trace");
    }
}
