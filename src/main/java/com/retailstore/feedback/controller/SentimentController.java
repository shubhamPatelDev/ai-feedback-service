package com.retailstore.feedback.controller;

import com.retailstore.feedback.service.SentimentAnalysisService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sentiment")
@Slf4j
@RequiredArgsConstructor
public class SentimentController {
    
    private final SentimentAnalysisService sentimentAnalysisService;
    
    @PostMapping("/analyze")
    public ResponseEntity<SentimentResponse> analyzeSentiment(@Valid @RequestBody SentimentRequest request) {
        try {
            log.debug("Analyzing sentiment for text: {}", request.getText().substring(0, Math.min(50, request.getText().length())));
            
            String sentiment = sentimentAnalysisService.analyzeSentiment(request.getText());
            
            SentimentResponse response = new SentimentResponse();
            response.setText(request.getText());
            response.setSentiment(sentiment);
            response.setSuccess(true);
            
            log.debug("Sentiment analysis result: {}", sentiment);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sentiment analysis request: {}", e.getMessage());
            
            SentimentResponse response = new SentimentResponse();
            response.setText(request.getText());
            response.setError(e.getMessage());
            response.setSuccess(false);
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            log.error("Error analyzing sentiment: {}", e.getMessage(), e);
            
            SentimentResponse response = new SentimentResponse();
            response.setText(request.getText());
            response.setError("Failed to analyze sentiment: " + e.getMessage());
            response.setSuccess(false);
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    @Data
    public static class SentimentRequest {
        @NotBlank(message = "Text is required")
        @Size(min = 1, max = 5000, message = "Text must be between 1 and 5000 characters")
        private String text;
    }
    
    @Data
    public static class SentimentResponse {
        private String text;
        private String sentiment;
        private boolean success;
        private String error;
    }
}
