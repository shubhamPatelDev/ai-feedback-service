package com.retailstore.feedback.exception;

public class FeedbackNotFoundException extends RuntimeException {
    
    public FeedbackNotFoundException(Long id) {
        super("Feedback not found with id: " + id);
    }
    
    public FeedbackNotFoundException(String message) {
        super(message);
    }
    
    public FeedbackNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
