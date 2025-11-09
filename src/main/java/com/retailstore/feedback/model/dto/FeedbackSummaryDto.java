package com.retailstore.feedback.model.dto;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FeedbackSummaryDto {
    private int totalFeedback;
    private Map<String, Integer> sentimentCounts;
    private Map<String, Integer> categoryCounts;
    private Map<String, Integer> departmentCounts;
    private List<FeedbackResponse> recentFeedback;
}
