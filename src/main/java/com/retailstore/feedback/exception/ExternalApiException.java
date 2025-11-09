package com.retailstore.feedback.exception;

public class ExternalApiException extends RuntimeException {
    
    private final int statusCode;
    
    public ExternalApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }
    
    public ExternalApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }
    
    public int getStatusCode() {
        return statusCode;
    }
}
