package com.retailstore.feedback.controller;

import com.retailstore.feedback.mapper.FeedbackMapper;
import com.retailstore.feedback.model.FeedbackEntry;
import com.retailstore.feedback.model.dto.FeedbackRequest;
import com.retailstore.feedback.service.SentimentAnalysisService;
import com.retailstore.feedback.service.async.AsyncFeedbackProcessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/api/v1/feedback/batch")
@Slf4j
@RequiredArgsConstructor
public class BatchFeedbackController {
    
    private final SentimentAnalysisService sentimentAnalysisService;
    private final AsyncFeedbackProcessor asyncFeedbackProcessor;
    
    @PostMapping
    public ResponseEntity<BatchProcessingResponse> processBatch(@Valid @RequestBody List<FeedbackRequest> requests) {
        try {
            if (requests.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            List<FeedbackEntry> entries = new ArrayList<>();
            for (FeedbackRequest request : requests) {
                FeedbackEntry entry = createFeedbackEntry(request);
                
                try {
                    String sentiment = sentimentAnalysisService.analyzeSentiment(entry.getComment());
                    entry.setSentiment(sentiment);
                } catch (Exception e) {
                    log.warn("Failed sentiment analysis for entry, using NEUTRAL: {}", e.getMessage());
                    entry.setSentiment("NEUTRAL");
                }
                
                entries.add(entry);
            }
            
            AtomicInteger progressCounter = new AtomicInteger(0);
            
            CompletableFuture<AsyncFeedbackProcessor.BatchProcessingResult> resultFuture = 
                asyncFeedbackProcessor.processBatchAsync(entries, new AsyncFeedbackProcessor.ProgressCallback() {
                    @Override
                    public void onProgress(int processed, int total, double percentage) {
                        progressCounter.set(processed);
                    }
                    
                    @Override
                    public void onComplete(AsyncFeedbackProcessor.BatchProcessingResult result) {
                    }
                });
            
            AsyncFeedbackProcessor.BatchProcessingResult result = resultFuture.join();
            
            BatchProcessingResponse response = new BatchProcessingResponse();
            response.setTotalSubmitted(requests.size());
            response.setSuccessful(result.getSuccessfulEntries());
            response.setFailed(result.getFailedEntries());
            response.setErrors(result.getErrors());
            response.setStatus("COMPLETED");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Error in batch processing: {}", e.getMessage(), e);
            
            BatchProcessingResponse response = new BatchProcessingResponse();
            response.setTotalSubmitted(requests.size());
            response.setStatus("FAILED");
            response.setErrors(List.of("Batch processing failed: " + e.getMessage()));
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    @PostMapping("/async")
    public ResponseEntity<AsyncBatchResponse> processBatchAsync(@Valid @RequestBody List<FeedbackRequest> requests) {
        try {
            if (requests.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            String batchId = "BATCH-" + System.currentTimeMillis();
            
            CompletableFuture.runAsync(() -> {
                try {
                    List<FeedbackEntry> entries = new ArrayList<>();
                    for (FeedbackRequest request : requests) {
                        FeedbackEntry entry = createFeedbackEntry(request);
                        String sentiment = sentimentAnalysisService.analyzeSentiment(entry.getComment());
                        entry.setSentiment(sentiment);
                        entries.add(entry);
                    }
                    
                    asyncFeedbackProcessor.processBatchAsync(entries, null).join();
                    
                } catch (Exception e) {
                    log.error("Error in async batch processing for batch ID {}: {}", batchId, e.getMessage(), e);
                }
            });
            
            AsyncBatchResponse response = new AsyncBatchResponse();
            response.setBatchId(batchId);
            response.setTotalSubmitted(requests.size());
            response.setStatus("PROCESSING");
            response.setMessage("Batch submitted successfully and is being processed");
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            log.error("Error starting async batch processing: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private FeedbackEntry createFeedbackEntry(FeedbackRequest request) {
        FeedbackEntry entry = new FeedbackEntry();
        entry.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        entry.setCustomer(request.getCustomer());
        entry.setDepartment(request.getDepartment());
        entry.setComment(request.getComment());
        entry.setDate(LocalDate.now());
        return entry;
    }
    
    public static class BatchProcessingResponse {
        private int totalSubmitted;
        private int successful;
        private int failed;
        private String status;
        private List<String> errors;
        
        public int getTotalSubmitted() {
            return totalSubmitted;
        }
        
        public void setTotalSubmitted(int totalSubmitted) {
            this.totalSubmitted = totalSubmitted;
        }
        
        public int getSuccessful() {
            return successful;
        }
        
        public void setSuccessful(int successful) {
            this.successful = successful;
        }
        
        public int getFailed() {
            return failed;
        }
        
        public void setFailed(int failed) {
            this.failed = failed;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public void setErrors(List<String> errors) {
            this.errors = errors;
        }
    }
    
    public static class AsyncBatchResponse {
        private String batchId;
        private int totalSubmitted;
        private String status;
        private String message;
        
        public String getBatchId() {
            return batchId;
        }
        
        public void setBatchId(String batchId) {
            this.batchId = batchId;
        }
        
        public int getTotalSubmitted() {
            return totalSubmitted;
        }
        
        public void setTotalSubmitted(int totalSubmitted) {
            this.totalSubmitted = totalSubmitted;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}
