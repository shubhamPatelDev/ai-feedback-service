package com.retailstore.feedback.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class SentimentConfig {
    
    public String getAnnotators() {
        return "tokenize,ssplit,pos,lemma,parse,sentiment";
    }
    
    public boolean isUseGpu() {
        return false;
    }
}
