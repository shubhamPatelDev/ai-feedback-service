package com.retailstore.feedback.controller;

import com.retailstore.feedback.mapper.EnhancedFeedbackMapper;
import com.retailstore.feedback.mapper.FeedbackMapper;
import com.retailstore.feedback.model.EnhancedFeedback;
import com.retailstore.feedback.model.FeedbackEntry;
import com.retailstore.feedback.model.dto.FeedbackRequest;
import com.retailstore.feedback.model.dto.FeedbackResponse;
import com.retailstore.feedback.service.FeedbackService;
import com.retailstore.feedback.service.SentimentAnalysisService;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/feedback")
@Slf4j
@RequiredArgsConstructor
public class RawFeedbackController {
    
    private final SentimentAnalysisService sentimentAnalysisService;
    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;
    private final EnhancedFeedbackMapper enhancedFeedbackMapper;

    @PostMapping("/raw")
    public Object processRawFeedback(@Valid @ModelAttribute FeedbackRequest request) {
        try {
            log.info("Processing raw feedback from customer: {}", request.getCustomer());
            
            FeedbackEntry entry = createFeedbackEntry(request);
            log.debug("Created feedback entry with ID: {}", entry.getId());
            
            String sentiment = sentimentAnalysisService.analyzeSentiment(entry.getComment());
            entry.setSentiment(sentiment);
            log.debug("Sentiment analysis complete: {}", sentiment);
            
            CompletableFuture<EnhancedFeedback> enhancedFuture = feedbackService.enhanceFeedbackAsync(entry);
            EnhancedFeedback enhanced = enhancedFuture.join();
            log.debug("AI enhancement complete for feedback ID: {}", enhanced.getId());
            
            feedbackService.saveEnhancedFeedback(enhanced);
            log.debug("Saved enhanced feedback to repository");
            
            log.info("Successfully processed feedback from customer: {}", request.getCustomer());
            
            return "redirect:/success";
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid feedback request: {}", e.getMessage());
            return "redirect:/submit?error=invalid";
            
        } catch (Exception e) {
            log.error("Error processing raw feedback: {}", e.getMessage(), e);
            return "redirect:/submit?error=server";
        }
    }
    
    @PostMapping("/raw/api")
    @ResponseBody
    public ResponseEntity<FeedbackResponse> processRawFeedbackApi(@Valid @RequestBody FeedbackRequest request) {
        try {
            log.info("Processing raw feedback from customer via API: {}", request.getCustomer());
            
            FeedbackEntry entry = createFeedbackEntry(request);
            log.debug("Created feedback entry with ID: {}", entry.getId());
            
            String sentiment = sentimentAnalysisService.analyzeSentiment(entry.getComment());
            entry.setSentiment(sentiment);
            log.debug("Sentiment analysis complete: {}", sentiment);
            
            CompletableFuture<EnhancedFeedback> enhancedFuture = feedbackService.enhanceFeedbackAsync(entry);
            EnhancedFeedback enhanced = enhancedFuture.join();
            log.debug("AI enhancement complete for feedback ID: {}", enhanced.getId());
            
            FeedbackResponse response = enhancedFeedbackMapper.toResponse(enhanced);
            log.info("Successfully processed feedback from customer: {}", request.getCustomer());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid feedback request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            log.error("Error processing raw feedback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/raw/async")
    public ResponseEntity<AsyncProcessingResponse> processRawFeedbackAsync(@Valid @RequestBody FeedbackRequest request) {
        try {
            log.info("Starting async processing for feedback from customer: {}", request.getCustomer());
            
            FeedbackEntry entry = createFeedbackEntry(request);
            
            CompletableFuture.runAsync(() -> {
                try {
                    String sentiment = sentimentAnalysisService.analyzeSentiment(entry.getComment());
                    entry.setSentiment(sentiment);
                    feedbackService.enhanceFeedbackAsync(entry).join();
                    log.info("Async processing complete for feedback ID: {}", entry.getId());
                } catch (Exception e) {
                    log.error("Error in async processing for feedback ID {}: {}", entry.getId(), e.getMessage(), e);
                }
            });
            
            AsyncProcessingResponse response = new AsyncProcessingResponse();
            response.setFeedbackId(entry.getId());
            response.setStatus("PROCESSING");
            response.setMessage("Feedback submitted successfully and is being processed");
            
            return ResponseEntity.accepted().body(response);
            
        } catch (Exception e) {
            log.error("Error starting async feedback processing: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    private FeedbackEntry createFeedbackEntry(FeedbackRequest request) {
        FeedbackEntry entry = feedbackMapper.toEntity(request);
        entry.setId(System.currentTimeMillis());
        entry.setDate(java.time.LocalDate.now());
        return entry;
    }
    
    @Data
    public static class AsyncProcessingResponse {
        private Long feedbackId;
        private String status;
        private String message;
    }
}
