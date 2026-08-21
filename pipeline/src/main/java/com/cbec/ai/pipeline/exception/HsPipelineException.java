package com.cbec.ai.pipeline.exception;

public class HsPipelineException extends RuntimeException {
    public HsPipelineException(String message) {
        super(message);
    }

    public HsPipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}
