package com.retailstore.feedback.model;

import java.time.LocalDateTime;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class EnhancedFeedback extends FeedbackEntry {
    private String category;
    private String actionableInsight;
    private LocalDateTime enhancedAt;

    public EnhancedFeedback(FeedbackEntry entry) {
        super(entry.getId(), entry.getCustomer(), entry.getDepartment(),
                entry.getDate(), entry.getComment(), entry.getSentiment());
        this.enhancedAt = LocalDateTime.now();
    }
}
