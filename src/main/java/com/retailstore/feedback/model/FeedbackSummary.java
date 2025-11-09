package com.retailstore.feedback.model;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackSummary {
    private int totalFeedback;
    private Map<String, Integer> sentimentCounts;
    private Map<String, Integer> categoryCounts;
    private Map<String, Integer> departmentCounts;
    private List<EnhancedFeedback> recentFeedback;
}
