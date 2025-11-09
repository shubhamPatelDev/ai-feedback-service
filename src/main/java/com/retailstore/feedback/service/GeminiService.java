package com.retailstore.feedback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.retailstore.feedback.config.GeminiConfig;
import com.retailstore.feedback.exception.ExternalApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeminiService {

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final GeminiConfig geminiConfig;
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public String generateContent(String prompt) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode content = objectMapper.createObjectNode();
            ArrayNode parts = objectMapper.createArrayNode();
            ObjectNode textPart = objectMapper.createObjectNode();

            textPart.put("text", prompt);
            parts.add(textPart);
            content.set("parts", parts);
            contents.add(content);
            requestBody.set("contents", contents);

            ObjectNode generationConfig = objectMapper.createObjectNode();
            generationConfig.put("temperature", 0.7);
            generationConfig.put("maxOutputTokens", 1024);
            requestBody.set("generationConfig", generationConfig);

            String url = geminiConfig.getApi().getUrl() + "?key=" + geminiConfig.getApiKey();

            Request request = new Request.Builder()
                    .url(url)
                    .post(RequestBody.create(requestBody.toString(), JSON))
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "No error body";
                    log.error("Gemini API call failed with status: {} - Response: {}", response.code(), errorBody);
                    throw new ExternalApiException(
                        "Gemini API returned error: " + response.code(), 
                        response.code()
                    );
                }

                String responseBody = response.body().string();
                return parseResponse(responseBody);
            }

        } catch (IOException e) {
            log.error("Error calling Gemini API: {}", e.getMessage(), e);
            throw new ExternalApiException("Failed to call Gemini API: " + e.getMessage(), 502);
        }
    }

    private String parseResponse(String responseBody) throws IOException {
        ObjectNode responseJson = (ObjectNode) objectMapper.readTree(responseBody);
        ArrayNode candidates = (ArrayNode) responseJson.get("candidates");

        if (candidates != null && candidates.size() > 0) {
            ObjectNode candidate = (ObjectNode) candidates.get(0);
            ObjectNode candidateContent = (ObjectNode) candidate.get("content");
            ArrayNode candidateParts = (ArrayNode) candidateContent.get("parts");

            if (candidateParts != null && candidateParts.size() > 0) {
                return candidateParts.get(0).get("text").asText();
            }
        }

        log.warn("No valid response from Gemini API");
        return "No response from Gemini";
    }

    public boolean testConnection() {
        try {
            String testPrompt = "Say 'Hello, World!' if you can hear me.";
            String response = generateContent(testPrompt);
            return response != null && !response.startsWith("Error");
        } catch (Exception e) {
            log.error("Gemini API connection test failed: {}", e.getMessage(), e);
            return false;
        }
    }
}
