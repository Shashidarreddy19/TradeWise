package com.trade.exception;

/**
 * Thrown when a user attempts to access a resource they don't own or have rights to.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
