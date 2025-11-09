package com.retailstore.feedback.service.async;

import com.retailstore.feedback.model.EnhancedFeedback;
import com.retailstore.feedback.model.FeedbackEntry;
import com.retailstore.feedback.service.FeedbackService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class AsyncFeedbackProcessor {
    
    private static final Logger log = LoggerFactory.getLogger(AsyncFeedbackProcessor.class);
    
    private final FeedbackService feedbackService;
    
    public AsyncFeedbackProcessor(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }
    
    @Async("feedbackExecutor")
    public CompletableFuture<BatchProcessingResult> processBatchAsync(
            List<FeedbackEntry> feedbackEntries,
            ProgressCallback progressCallback) {
        
        log.info("Starting batch processing for {} feedback entries", feedbackEntries.size());
        
        BatchProcessingResult result = new BatchProcessingResult();
        result.setTotalEntries(feedbackEntries.size());
        
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        
        List<CompletableFuture<EnhancedFeedback>> futures = new ArrayList<>();
        List<EnhancedFeedback> successfulResults = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        
        for (FeedbackEntry entry : feedbackEntries) {
            CompletableFuture<EnhancedFeedback> future = feedbackService.enhanceFeedbackAsync(entry)
                .whenComplete((enhanced, throwable) -> {
                    int processed = processedCount.incrementAndGet();
                    
                    if (throwable == null) {
                        successCount.incrementAndGet();
                        synchronized (successfulResults) {
                            successfulResults.add(enhanced);
                        }
                    } else {
                        failureCount.incrementAndGet();
                        synchronized (errors) {
                            errors.add("Error processing feedback ID " + entry.getId() + ": " + throwable.getMessage());
                        }
                        log.warn("Failed to process feedback ID {}: {}", entry.getId(), throwable.getMessage());
                    }
                    
                    if (progressCallback != null) {
                        double progress = (double) processed / feedbackEntries.size() * 100;
                        progressCallback.onProgress(processed, feedbackEntries.size(), progress);
                    }
                });
            
            futures.add(future);
        }
        
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );
        
        return allFutures.thenApply(v -> {
            result.setProcessedEntries(processedCount.get());
            result.setSuccessfulEntries(successCount.get());
            result.setFailedEntries(failureCount.get());
            result.setResults(successfulResults);
            result.setErrors(errors);
            result.setCompleted(true);
            
            log.info("Batch processing completed: {} successful, {} failed out of {} total",
                    successCount.get(), failureCount.get(), feedbackEntries.size());
            
            if (progressCallback != null) {
                progressCallback.onComplete(result);
            }
            
            return result;
        }).exceptionally(ex -> {
            log.error("Batch processing failed with exception: {}", ex.getMessage(), ex);
            result.setCompleted(false);
            result.setErrors(List.of("Batch processing failed: " + ex.getMessage()));
            return result;
        });
    }
    
    public interface ProgressCallback {
        void onProgress(int processed, int total, double percentage);
        
        void onComplete(BatchProcessingResult result);
    }
    
    public static class BatchProcessingResult {
        private int totalEntries;
        private int processedEntries;
        private int successfulEntries;
        private int failedEntries;
        private List<EnhancedFeedback> results;
        private List<String> errors;
        private boolean completed;
        
        public BatchProcessingResult() {
            this.results = new ArrayList<>();
            this.errors = new ArrayList<>();
        }
        
        public int getTotalEntries() {
            return totalEntries;
        }
        
        public void setTotalEntries(int totalEntries) {
            this.totalEntries = totalEntries;
        }
        
        public int getProcessedEntries() {
            return processedEntries;
        }
        
        public void setProcessedEntries(int processedEntries) {
            this.processedEntries = processedEntries;
        }
        
        public int getSuccessfulEntries() {
            return successfulEntries;
        }
        
        public void setSuccessfulEntries(int successfulEntries) {
            this.successfulEntries = successfulEntries;
        }
        
        public int getFailedEntries() {
            return failedEntries;
        }
        
        public void setFailedEntries(int failedEntries) {
            this.failedEntries = failedEntries;
        }
        
        public List<EnhancedFeedback> getResults() {
            return results;
        }
        
        public void setResults(List<EnhancedFeedback> results) {
            this.results = results;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
        
        public boolean isCompleted() {
            return completed;
        }
        
        public void setCompleted(boolean completed) {
            this.completed = completed;
        }
    }
}
