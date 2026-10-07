package com.personal.happygallery.bootstrap.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

class SensitiveLogMaskerTest {

    private static final String SIGNED_ACCESS_TOKEN =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY6MTc3NzU5MzYwMA."
                    + "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ";
    private static final String BOT_TOKEN = "0.test-token_with.dot-1234";

    @DisplayName("자동 입력 방지 토큰은 헤더 대소문자와 값 구분자에 관계없이 마스킹한다")
    @Test
    void mask_botToken_preservesLogStructure() {
        String message = "{\"X-Bot-Token\":\"%s\"} x-bot-token=%s, action=phone_verification"
                .formatted(BOT_TOKEN, BOT_TOKEN);

        assertThat(SensitiveLogMasker.mask(message))
                .doesNotContain(BOT_TOKEN)
                .contains("\"X-Bot-Token\":\"***\"")
                .contains("x-bot-token=***, action=phone_verification");
    }

    @DisplayName("실제 Base64URL 서명 비회원 토큰은 헤더 구분자와 JSON 따옴표를 보존해 마스킹한다")
    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {"X-Access-Token", "X-Payment-Status-Token"})
    void mask_signedGuestToken_preservesLogStructure(String header) {
        String message = """
                headers={"%1$s":"%2$s"} %1$s: %2$s trace.id=value.with.dot
                """.formatted(header, SIGNED_ACCESS_TOKEN);

        String masked = SensitiveLogMasker.mask(message);

        assertThat(masked)
                .doesNotContain(SIGNED_ACCESS_TOKEN)
                .contains("\"" + header + "\":\"***\"")
                .contains(header + ": ***")
                .contains("trace.id=value.with.dot");
    }

    @DisplayName("운영 JSON 로그는 message와 stack_trace의 토큰을 마스킹하고 기존 필드를 유지한다")
    @Test
    void productionJsonEncoder_masksMessageAndStackTrace() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader()
                .load("application-prod", new ClassPathResource("application-prod.yml"))
                .forEach(environment.getPropertySources()::addLast);
        LoggerContext context = new LoggerContext();
        context.putObject(Environment.class.getName(), environment);
        StructuredLogEncoder encoder = new StructuredLogEncoder();
        encoder.setContext(context);
        encoder.setFormat("logstash");
        encoder.start();
        LoggingEvent event = new LoggingEvent(
                Logger.class.getName(),
                context.getLogger("com.personal.happygallery.payment"),
                Level.ERROR,
                "결제 확인 실패 X-Access-Token=" + SIGNED_ACCESS_TOKEN,
                new IllegalStateException("invalid X-Bot-Token=" + BOT_TOKEN),
                null);
        event.setMDCPropertyMap(Map.of("requestId", "req-1"));

        String json;
        try {
            json = new String(encoder.encode(event), StandardCharsets.UTF_8);
        } finally {
            encoder.stop();
            context.stop();
        }

        assertThat(json).doesNotContain(SIGNED_ACCESS_TOKEN).doesNotContain(BOT_TOKEN);
        assertThat((String) JsonPath.read(json, "$.message")).isEqualTo("결제 확인 실패 X-Access-Token=***");
        assertThat((String) JsonPath.read(json, "$.stack_trace"))
                .contains("IllegalStateException", "X-Bot-Token=***");
        assertThat((String) JsonPath.read(json, "$.level")).isEqualTo("ERROR");
        assertThat((String) JsonPath.read(json, "$.logger_name")).isEqualTo("com.personal.happygallery.payment");
        assertThat((String) JsonPath.read(json, "$.requestId")).isEqualTo("req-1");
        assertThat((String) JsonPath.read(json, "$.service")).isEqualTo("happygallery");
        assertThat((String) JsonPath.read(json, "$['@timestamp']")).endsWith("+09:00");
        assertThat(JsonPath.<Map<String, Object>>read(json, "$"))
                .containsKey("thread_name")
                .doesNotContainKeys("@version", "level_value");
    }
}
