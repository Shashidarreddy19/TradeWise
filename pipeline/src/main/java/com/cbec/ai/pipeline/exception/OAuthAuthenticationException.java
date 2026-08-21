package com.cbec.ai.pipeline.exception;

public class OAuthAuthenticationException extends RuntimeException {

    private final int statusCode;

    public OAuthAuthenticationException(String message) {
        super(message);
        this.statusCode = 401;
    }

    public OAuthAuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 401;
    }

    public OAuthAuthenticationException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
