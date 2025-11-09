package com.retailstore.feedback.repository;

import com.retailstore.feedback.model.FeedbackEntry;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public interface FeedbackRepository {
    
    List<FeedbackEntry> findAll() throws IOException;
    
    Optional<FeedbackEntry> findById(Long id);
    
    FeedbackEntry save(FeedbackEntry feedback) throws IOException;
    
    void deleteById(Long id) throws IOException;
    
    List<FeedbackEntry> findByDepartment(String department);
    
    List<FeedbackEntry> findBySentiment(String sentiment);
}
