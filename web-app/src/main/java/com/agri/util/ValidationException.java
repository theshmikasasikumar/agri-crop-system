package com.agri.util;

/** Thrown when user input breaks a business rule (unknown id, bad date, area too large, ...). */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
