package com.trade.exception;

/**
 * Thrown for invalid business-logic requests (e.g., duplicate email, invalid state transitions).
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
