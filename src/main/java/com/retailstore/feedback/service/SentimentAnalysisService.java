package com.retailstore.feedback.service;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
@Slf4j
public class SentimentAnalysisService {

    private StanfordCoreNLP pipeline;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,parse,sentiment");
        this.pipeline = new StanfordCoreNLP(props);
    }

    public String analyzeSentiment(String comment) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("Comment cannot be null or empty");
        }

        CoreDocument doc = new CoreDocument(comment);
        pipeline.annotate(doc);
        List<CoreSentence> sentences = doc.sentences();

        if (sentences.isEmpty()) {
            return "NEUTRAL";
        }

        Map<String, Integer> sentimentCounts = new HashMap<>();
        for (CoreSentence sentence : sentences) {
            String sentiment = sentence.sentiment();
            sentimentCounts.put(sentiment, sentimentCounts.getOrDefault(sentiment, 0) + 1);
        }

        return sentimentCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("NEUTRAL");
    }

    @PreDestroy
    public void cleanup() {
    }
}
