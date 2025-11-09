package com.retailstore.feedback.exception;

public class SentimentAnalysisException extends RuntimeException {
    
    public SentimentAnalysisException(String message) {
        super(message);
    }
    
    public SentimentAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
