package com.retailstore.feedback.mapper;

import com.retailstore.feedback.model.EnhancedFeedback;
import com.retailstore.feedback.model.dto.FeedbackResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface EnhancedFeedbackMapper {
    
    FeedbackResponse toResponse(EnhancedFeedback feedback);
}
