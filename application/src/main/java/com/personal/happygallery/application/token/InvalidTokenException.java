package com.personal.happygallery.application.token;

public final class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message, null, false, false);
    }
}
