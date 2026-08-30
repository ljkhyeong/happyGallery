package com.personal.happygallery.adapter.in.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public final class Utf8ByteLengthValidator
        implements ConstraintValidator<Utf8ByteLength, CharSequence> {

    private int max;

    @Override
    public void initialize(Utf8ByteLength constraint) {
        max = constraint.max();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || value.toString().getBytes(StandardCharsets.UTF_8).length <= max;
    }
}
