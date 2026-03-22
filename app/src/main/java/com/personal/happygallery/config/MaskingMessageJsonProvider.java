package com.personal.happygallery.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.fasterxml.jackson.core.JsonGenerator;
import net.logstash.logback.composite.AbstractFieldJsonProvider;
import net.logstash.logback.composite.JsonWritingUtils;

import java.io.IOException;

/**
 * LogstashEncoder용 커스텀 message provider.
 *
 * <p>기본 {@code MessageJsonProvider}를 대체하여 민감 데이터를 마스킹한 뒤
 * JSON {@code "message"} 필드로 출력한다.
 */
public class MaskingMessageJsonProvider extends AbstractFieldJsonProvider<ILoggingEvent> {

    public static final String FIELD_MESSAGE = "message";

    public MaskingMessageJsonProvider() {
        setFieldName(FIELD_MESSAGE);
    }

    @Override
    public void writeTo(JsonGenerator generator, ILoggingEvent event) throws IOException {
        String masked = MaskingPatternLayout.maskSensitive(event.getFormattedMessage());
        JsonWritingUtils.writeStringField(generator, getFieldName(), masked);
    }
}
