package com.retailstore.feedback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiConfig {
    private String apiKey;
    private Api api = new Api();
    
    @Data
    public static class Api {
        private String url;
        private int timeout = 30;
    }
}
