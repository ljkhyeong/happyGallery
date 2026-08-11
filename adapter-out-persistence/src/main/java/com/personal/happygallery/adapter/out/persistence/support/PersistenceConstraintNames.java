package com.personal.happygallery.adapter.out.persistence.support;

import java.util.Locale;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.util.StringUtils;

/** Hibernate가 번역한 제약 조건 이름을 저장소 전반에서 같은 규칙으로 정규화한다. */
public final class PersistenceConstraintNames {

    private PersistenceConstraintNames() {}

    public static boolean matches(Throwable throwable, String expectedConstraint) {
        return find(throwable).filter(expectedConstraint::equals).isPresent();
    }

    public static Optional<String> find(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation
                    && StringUtils.hasText(violation.getConstraintName())) {
                return Optional.of(StringUtils.unqualify(violation.getConstraintName()
                        .toLowerCase(Locale.ROOT)
                        .replace("`", "")
                        .replace("\"", "")
                        .replace("'", "")));
            }
            current = current.getCause();
        }
        return Optional.empty();
    }
}
