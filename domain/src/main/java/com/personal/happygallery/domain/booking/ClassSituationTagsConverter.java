package com.personal.happygallery.domain.booking;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 고정된 소수의 상황 태그를 `classes.situation_tags` 한 컬럼에 쉼표로 저장한다. 태그가 없으면 NULL이다.
 * OSIV가 꺼진 응답 변환에서도 추가 조회 없이 읽도록 별도 컬렉션 테이블을 쓰지 않는다.
 */
@Converter
public class ClassSituationTagsConverter implements AttributeConverter<Set<ClassSituationTag>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(Set<ClassSituationTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return null;
        }
        return EnumSet.copyOf(tags).stream()
                .map(Enum::name)
                .collect(Collectors.joining(DELIMITER));
    }

    @Override
    public Set<ClassSituationTag> convertToEntityAttribute(String value) {
        if (value == null || value.isBlank()) {
            return Collections.unmodifiableSet(EnumSet.noneOf(ClassSituationTag.class));
        }
        EnumSet<ClassSituationTag> tags = EnumSet.noneOf(ClassSituationTag.class);
        Arrays.stream(value.split(DELIMITER))
                .map(String::strip)
                .filter(name -> !name.isEmpty())
                .map(ClassSituationTag::valueOf)
                .forEach(tags::add);
        return Collections.unmodifiableSet(tags);
    }
}
