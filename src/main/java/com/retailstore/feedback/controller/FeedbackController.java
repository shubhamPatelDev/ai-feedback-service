package com.retailstore.feedback.controller;

import com.retailstore.feedback.mapper.EnhancedFeedbackMapper;
import com.retailstore.feedback.mapper.FeedbackMapper;
import com.retailstore.feedback.model.EnhancedFeedback;
import com.retailstore.feedback.model.FeedbackEntry;
import com.retailstore.feedback.model.FeedbackSummary;
import com.retailstore.feedback.model.dto.FeedbackRequest;
import com.retailstore.feedback.model.dto.FeedbackResponse;
import com.retailstore.feedback.model.dto.FeedbackSummaryDto;
import com.retailstore.feedback.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@Slf4j
@RequiredArgsConstructor
public class FeedbackController {
    
    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;
    private final EnhancedFeedbackMapper enhancedFeedbackMapper;

    @GetMapping("/")
    public String dashboard(Model model) {
        try {
            List<EnhancedFeedback> enhancedFeedback = feedbackService.getEnhancedFeedback();
            FeedbackSummary summary = feedbackService.generateFeedbackSummary();
            
            model.addAttribute("enhancedFeedback", enhancedFeedback);
            model.addAttribute("summary", summary);
            
            return "dashboard";
        } catch (IOException e) {
            log.error("Error loading feedback data for dashboard: {}", e.getMessage(), e);
            model.addAttribute("error", "Error loading feedback data: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/submit")
    public String submitForm(Model model) {
        model.addAttribute("feedbackRequest", FeedbackRequest.builder().build());
        return "submit";
    }

    @GetMapping("/success")
    public String successPage() {
        return "success";
    }

    @GetMapping("/api/v1/feedback")
    @ResponseBody
    public ResponseEntity<List<FeedbackResponse>> getAllFeedback() {
        try {
            List<EnhancedFeedback> enhancedFeedback = feedbackService.getEnhancedFeedback();
            List<FeedbackResponse> responses = enhancedFeedback.stream()
                    .map(enhancedFeedbackMapper::toResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(responses);
        } catch (IOException e) {
            log.error("Error fetching feedback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/v1/feedback/summary")
    @ResponseBody
    public ResponseEntity<FeedbackSummaryDto> getFeedbackSummary() {
        try {
            FeedbackSummary summary = feedbackService.generateFeedbackSummary();
            
            List<FeedbackResponse> recentResponses = summary.getRecentFeedback().stream()
                    .map(enhancedFeedbackMapper::toResponse)
                    .collect(Collectors.toList());
            
            FeedbackSummaryDto dto = FeedbackSummaryDto.builder()
                    .totalFeedback(summary.getTotalFeedback())
                    .sentimentCounts(summary.getSentimentCounts())
                    .categoryCounts(summary.getCategoryCounts())
                    .departmentCounts(summary.getDepartmentCounts())
                    .recentFeedback(recentResponses)
                    .build();
            
            return ResponseEntity.ok(dto);
        } catch (IOException e) {
            log.error("Error generating feedback summary: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/api/v1/feedback/refresh")
    @ResponseBody
    public ResponseEntity<String> refreshFeedback() {
        feedbackService.clearCache();
        return ResponseEntity.ok("Feedback cache cleared successfully");
    }
}
