package com.retailstore.feedback.mapper;

import com.retailstore.feedback.model.FeedbackEntry;
import com.retailstore.feedback.model.dto.FeedbackRequest;
import com.retailstore.feedback.model.dto.FeedbackResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.time.LocalDate;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface FeedbackMapper {
    
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "actionableInsight", ignore = true)
    @Mapping(target = "enhancedAt", ignore = true)
    FeedbackResponse toResponse(FeedbackEntry entry);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "date", expression = "java(java.time.LocalDate.now())")
    @Mapping(target = "sentiment", ignore = true)
    FeedbackEntry toEntity(FeedbackRequest request);
}
