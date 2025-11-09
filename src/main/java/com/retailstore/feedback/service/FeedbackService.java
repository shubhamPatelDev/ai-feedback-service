package com.retailstore.feedback.service;

import com.retailstore.feedback.mapper.EnhancedFeedbackMapper;
import com.retailstore.feedback.model.EnhancedFeedback;
import com.retailstore.feedback.model.FeedbackEntry;
import com.retailstore.feedback.model.FeedbackSummary;
import com.retailstore.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeedbackService {
    
    private final FeedbackRepository feedbackRepository;
    private final GeminiService geminiService;

    private List<EnhancedFeedback> enhancedFeedbackCache = null;

    public synchronized List<EnhancedFeedback> getEnhancedFeedback() throws IOException {
        if (enhancedFeedbackCache != null) {
            return enhancedFeedbackCache;
        }

        List<FeedbackEntry> entries = feedbackRepository.findAll();
        List<EnhancedFeedback> enhancedEntries = new ArrayList<>();

        for (FeedbackEntry entry : entries) {
            EnhancedFeedback enhancedEntry = enhanceFeedback(entry);
            enhancedEntries.add(enhancedEntry);
        }

        enhancedFeedbackCache = enhancedEntries;
        return enhancedEntries;
    }

    private EnhancedFeedback enhanceFeedback(FeedbackEntry entry) {
        EnhancedFeedback enhancedEntry = new EnhancedFeedback(entry);

        String prompt = String.format("""
            You are an AI assistant specialized in customer feedback analysis.
            Analyze the following customer feedback and:
            1. Categorize the feedback into one of these categories: Product Quality, Customer Service, Store Experience, Website/App, Delivery, Price/Value, Inventory/Stock, or Other.
            2. Provide a specific actionable insight or recommendation based on the feedback.
            
            Format your response as JSON with two fields: "category" and "actionableInsight".
            Keep your response concise but insightful.
            
            Customer Feedback:
            Comment: %s
            Department: %s
            Sentiment: %s
            
            Provide the category and actionable insight as JSON:
            """, entry.getComment(), entry.getDepartment(), entry.getSentiment());

        try {
            String response = geminiService.generateContent(prompt);
            String jsonResponse = response.trim();

            Pattern categoryPattern = Pattern.compile("\"category\"\\s*:\\s*\"([^\"]+)\"");
            Matcher categoryMatcher = categoryPattern.matcher(jsonResponse);
            if (categoryMatcher.find()) {
                enhancedEntry.setCategory(categoryMatcher.group(1));
            } else {
                enhancedEntry.setCategory("Uncategorized");
            }

            Pattern insightPattern = Pattern.compile("\"actionableInsight\"\\s*:\\s*\"([^\"]+)\"");
            Matcher insightMatcher = insightPattern.matcher(jsonResponse);
            if (insightMatcher.find()) {
                enhancedEntry.setActionableInsight(insightMatcher.group(1));
            } else {
                enhancedEntry.setActionableInsight("No specific action recommended.");
            }

        } catch (Exception e) {
            log.warn("Failed to enhance feedback ID {}: {}", entry.getId(), e.getMessage());
            enhancedEntry.setCategory("Error in processing");
            enhancedEntry.setActionableInsight("Could not generate insight due to API error: " + e.getMessage());
        }

        enhancedEntry.setEnhancedAt(LocalDateTime.now());
        return enhancedEntry;
    }

    public FeedbackSummary generateFeedbackSummary() throws IOException {
        List<EnhancedFeedback> allFeedback = getEnhancedFeedback();

        Map<String, Integer> sentimentCounts = new HashMap<>();
        for (EnhancedFeedback feedback : allFeedback) {
            String sentiment = feedback.getSentiment();
            sentimentCounts.put(sentiment, sentimentCounts.getOrDefault(sentiment, 0) + 1);
        }

        Map<String, Integer> categoryCounts = new HashMap<>();
        for (EnhancedFeedback feedback : allFeedback) {
            String category = feedback.getCategory();
            categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
        }

        Map<String, Integer> departmentCounts = new HashMap<>();
        for (EnhancedFeedback feedback : allFeedback) {
            String department = feedback.getDepartment();
            departmentCounts.put(department, departmentCounts.getOrDefault(department, 0) + 1);
        }

        List<EnhancedFeedback> recentFeedback = allFeedback.stream()
                .sorted(Comparator.comparing(EnhancedFeedback::getId).reversed())
                .limit(5)
                .collect(Collectors.toList());

        return FeedbackSummary.builder()
                .totalFeedback(allFeedback.size())
                .sentimentCounts(sentimentCounts)
                .categoryCounts(categoryCounts)
                .departmentCounts(departmentCounts)
                .recentFeedback(recentFeedback)
                .build();
    }

    @Async("feedbackExecutor")
    public CompletableFuture<EnhancedFeedback> enhanceFeedbackAsync(FeedbackEntry entry) {
        try {
            EnhancedFeedback enhanced = enhanceFeedback(entry);
            return CompletableFuture.completedFuture(enhanced);
        } catch (Exception e) {
            log.error("Error enhancing feedback asynchronously for ID {}: {}", entry.getId(), e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    public void saveEnhancedFeedback(EnhancedFeedback enhanced) {
        try {
            FeedbackEntry entry = new FeedbackEntry();
            entry.setId(enhanced.getId());
            entry.setDate(enhanced.getDate() != null ? enhanced.getDate() : java.time.LocalDate.now());
            entry.setCustomer(enhanced.getCustomer());
            entry.setDepartment(enhanced.getDepartment());
            entry.setComment(enhanced.getComment());
            entry.setSentiment(enhanced.getSentiment());
            
            feedbackRepository.save(entry);
            clearCache();
            log.info("Saved enhanced feedback ID: {}", enhanced.getId());
        } catch (IOException e) {
            log.error("Error saving enhanced feedback: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save feedback", e);
        }
    }

    public void clearCache() {
        enhancedFeedbackCache = null;
    }
}
