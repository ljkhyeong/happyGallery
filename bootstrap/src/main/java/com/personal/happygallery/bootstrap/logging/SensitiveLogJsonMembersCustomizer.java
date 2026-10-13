package com.personal.happygallery.bootstrap.logging;

import com.personal.happygallery.domain.time.Clocks;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import org.springframework.boot.json.JsonWriter.Members;
import org.springframework.boot.json.JsonWriter.MemberPath;
import org.springframework.boot.json.JsonWriter.ValueProcessor;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * 운영 JSON 로그(Spring Boot logstash 형식)의 {@code message}·{@code stack_trace}에 공통 민감정보 마스킹을 적용하고
 * {@code @timestamp}를 서울 시간대로 기록한다.
 *
 * <p>{@code logging.structured.json.customizer}로 등록하며 Boot가 리플렉션으로 생성한다.
 */
public class SensitiveLogJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

    private static final Set<MemberPath> MASKED_MEMBERS =
            Set.of(MemberPath.of("message"), MemberPath.of("stack_trace"));
    private static final MemberPath TIMESTAMP = MemberPath.of("@timestamp");

    @Override
    public void customize(Members<Object> members) {
        members.applyingValueProcessor(ValueProcessor.of(String.class, SensitiveLogMasker::mask)
                .whenHasPath(MASKED_MEMBERS::contains));
        members.applyingValueProcessor(ValueProcessor.of(String.class, SensitiveLogJsonMembersCustomizer::toSeoulTime)
                .whenHasPath(TIMESTAMP::equals));
    }

    private static String toSeoulTime(String timestamp) {
        return OffsetDateTime.parse(timestamp)
                .atZoneSameInstant(Clocks.SEOUL)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}
