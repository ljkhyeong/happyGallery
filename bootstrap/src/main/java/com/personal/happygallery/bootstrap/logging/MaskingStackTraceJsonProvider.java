package com.personal.happygallery.bootstrap.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import java.io.IOException;
import net.logstash.logback.composite.JsonWritingUtils;
import net.logstash.logback.composite.loggingevent.StackTraceJsonProvider;

/** 운영 JSON 로그의 throwable 문자열에도 공통 민감정보 마스킹을 적용한다. */
public class MaskingStackTraceJsonProvider extends StackTraceJsonProvider {

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        if (event.getThrowableProxy() == null) {
            return;
        }
        String stackTrace = getThrowableConverter().convert(event);
        JsonWritingUtils.writeStringField(
                generator, getFieldName(), MaskingPatternLayout.maskSensitive(stackTrace));
    }
}
