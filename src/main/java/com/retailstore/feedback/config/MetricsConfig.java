package com.retailstore.feedback.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {
    
    @Bean
    public Counter feedbackProcessedCounter(MeterRegistry meterRegistry) {
        return Counter.builder("feedback.processed")
            .description("Number of feedback entries processed")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter aiEnhancementCounter(MeterRegistry meterRegistry) {
        return Counter.builder("ai.enhancement")
            .description("Number of AI enhancement operations")
            .register(meterRegistry);
    }
    
    @Bean
    public Timer aiEnhancementTimer(MeterRegistry meterRegistry) {
        return Timer.builder("ai.enhancement.duration")
            .description("Time taken for AI enhancement operations")
            .register(meterRegistry);
    }
    
    @Bean
    public Counter sentimentAnalysisCounter(MeterRegistry meterRegistry) {
        return Counter.builder("sentiment.analysis")
            .description("Number of sentiment analysis operations")
            .register(meterRegistry);
    }
}
