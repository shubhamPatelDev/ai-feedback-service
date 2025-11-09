package com.retailstore.feedback.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackEntry {
    private Long id;
    private String customer;
    private String department;
    private LocalDate date;
    private String comment;
    private String sentiment;
}
