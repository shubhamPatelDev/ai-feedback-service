package com.retailstore.feedback.repository.impl;

import com.retailstore.feedback.model.FeedbackEntry;
import com.retailstore.feedback.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Repository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Repository
@Slf4j
@RequiredArgsConstructor
public class FileFeedbackRepository implements FeedbackRepository {

    private final ResourceLoader resourceLoader;

    @Value("${feedback.file.path:classpath:data/sentiment_feedback_output.txt}")
    private String feedbackFilePath;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    private static final Pattern FEEDBACK_PATTERN = Pattern.compile("Feedback #(\\d+)");
    private static final Pattern CUSTOMER_PATTERN = Pattern.compile("Customer:\\s*(.+)");
    private static final Pattern DEPARTMENT_PATTERN = Pattern.compile("Department:\\s*(.+)");
    private static final Pattern DATE_PATTERN = Pattern.compile("Date:\\s*(.+)");
    private static final Pattern COMMENT_PATTERN = Pattern.compile("Comment:\\s*(.+)");
    private static final Pattern SENTIMENT_PATTERN = Pattern.compile("Sentiment:\\s*(.+)");
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public List<FeedbackEntry> findAll() throws IOException {
        lock.readLock().lock();
        try {
            return readFeedbackFromFile();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Optional<FeedbackEntry> findById(Long id) {
        lock.readLock().lock();
        try {
            List<FeedbackEntry> allFeedback = readFeedbackFromFile();
            return allFeedback.stream()
                    .filter(entry -> entry.getId().equals(id))
                    .findFirst();
        } catch (IOException e) {
            log.error("Error finding feedback by ID {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public FeedbackEntry save(FeedbackEntry feedback) throws IOException {
        lock.writeLock().lock();
        try {
            List<FeedbackEntry> allFeedback = readFeedbackFromFile();
            
            if (feedback.getId() == null) {
                Long maxId = allFeedback.stream()
                        .map(FeedbackEntry::getId)
                        .max(Long::compareTo)
                        .orElse(0L);
                feedback.setId(maxId + 1);
                allFeedback.add(feedback);
            } else {
                allFeedback.removeIf(f -> f.getId().equals(feedback.getId()));
                allFeedback.add(feedback);
            }
            
            writeFeedbackToFile(allFeedback);
            log.info("Saved feedback with ID: {}", feedback.getId());
            return feedback;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void deleteById(Long id) throws IOException {
        lock.writeLock().lock();
        try {
            List<FeedbackEntry> allFeedback = readFeedbackFromFile();
            allFeedback.removeIf(f -> f.getId().equals(id));
            writeFeedbackToFile(allFeedback);
            log.info("Deleted feedback with ID: {}", id);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<FeedbackEntry> findByDepartment(String department) {
        lock.readLock().lock();
        try {
            List<FeedbackEntry> allFeedback = readFeedbackFromFile();
            return allFeedback.stream()
                    .filter(entry -> entry.getDepartment() != null && 
                            entry.getDepartment().equalsIgnoreCase(department))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error finding feedback by department {}: {}", department, e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<FeedbackEntry> findBySentiment(String sentiment) {
        lock.readLock().lock();
        try {
            List<FeedbackEntry> allFeedback = readFeedbackFromFile();
            return allFeedback.stream()
                    .filter(entry -> entry.getSentiment() != null && 
                            entry.getSentiment().equalsIgnoreCase(sentiment))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.error("Error finding feedback by sentiment {}: {}", sentiment, e.getMessage(), e);
            return new ArrayList<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    private List<FeedbackEntry> readFeedbackFromFile() throws IOException {
        Resource resource = resourceLoader.getResource(feedbackFilePath);
        
        if (!resource.exists()) {
            log.warn("Feedback file not found: {}", feedbackFilePath);
            return new ArrayList<>();
        }
        
        List<FeedbackEntry> entries = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {
            StringBuilder entryText = new StringBuilder();
            String line;

            boolean inDetailedSection = false;
            while ((line = reader.readLine()) != null) {
                if (line.contains("## Detailed Feedback Entries")) {
                    inDetailedSection = true;
                    continue;
                }

                if (!inDetailedSection) {
                    continue;
                }

                if (line.trim().isEmpty() && entryText.length() > 0) {
                    FeedbackEntry entry = parseFeedbackEntry(entryText.toString());
                    if (entry != null) {
                        entries.add(entry);
                    }
                    entryText = new StringBuilder();
                } else {
                    entryText.append(line).append("\n");
                }
            }

            if (entryText.length() > 0) {
                FeedbackEntry entry = parseFeedbackEntry(entryText.toString());
                if (entry != null) {
                    entries.add(entry);
                }
            }
        }

        return entries;
    }

    private void writeFeedbackToFile(List<FeedbackEntry> entries) throws IOException {
        String path = feedbackFilePath.replace("classpath:", "").replace("file:", "");
        Path filePath = Paths.get(path);
        
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            
            writer.write("# Customer Feedback Analysis\n\n");
            writer.write("## Detailed Feedback Entries\n\n");
            
            for (FeedbackEntry entry : entries) {
                writer.write(String.format("Feedback #%d\n", entry.getId()));
                writer.write(String.format("Customer: %s\n", entry.getCustomer()));
                writer.write(String.format("Department: %s\n", entry.getDepartment()));
                writer.write(String.format("Date: %s\n", entry.getDate().format(DATE_FORMATTER)));
                writer.write(String.format("Comment: %s\n", entry.getComment()));
                writer.write(String.format("Sentiment: %s\n", entry.getSentiment()));
                writer.write("\n");
            }
        }
    }

    private FeedbackEntry parseFeedbackEntry(String text) {
        Long id = null;
        String customer = null;
        String department = null;
        LocalDate date = null;
        String comment = null;
        String sentiment = null;

        Matcher idMatcher = FEEDBACK_PATTERN.matcher(text);
        if (idMatcher.find()) {
            id = Long.parseLong(idMatcher.group(1));
        } else {
            return null;
        }

        Matcher customerMatcher = CUSTOMER_PATTERN.matcher(text);
        if (customerMatcher.find()) {
            customer = customerMatcher.group(1).trim();
        }

        Matcher departmentMatcher = DEPARTMENT_PATTERN.matcher(text);
        if (departmentMatcher.find()) {
            department = departmentMatcher.group(1).trim();
        }

        Matcher dateMatcher = DATE_PATTERN.matcher(text);
        if (dateMatcher.find()) {
            String dateStr = dateMatcher.group(1).trim();
            try {
                date = LocalDate.parse(dateStr, DATE_FORMATTER);
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse date: {}", dateStr);
                date = LocalDate.now();
            }
        }

        Matcher commentMatcher = COMMENT_PATTERN.matcher(text);
        if (commentMatcher.find()) {
            comment = commentMatcher.group(1).trim();
        }

        Matcher sentimentMatcher = SENTIMENT_PATTERN.matcher(text);
        if (sentimentMatcher.find()) {
            sentiment = sentimentMatcher.group(1).trim();
        }

        return FeedbackEntry.builder()
                .id(id)
                .customer(customer)
                .department(department)
                .date(date)
                .comment(comment)
                .sentiment(sentiment)
                .build();
    }
}
