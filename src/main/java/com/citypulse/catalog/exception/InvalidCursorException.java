package com.citypulse.catalog.exception;

public class InvalidCursorException extends RuntimeException {

    public InvalidCursorException(Throwable cause) {
        super("The pagination cursor is invalid", cause);
    }
}