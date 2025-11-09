package com.retailstore.feedback.model.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackResponse {
    private Long id;
    private String customer;
    private String department;
    private LocalDate date;
    private String comment;
    private String sentiment;
    private String category;
    private String actionableInsight;
    private LocalDateTime enhancedAt;
}
