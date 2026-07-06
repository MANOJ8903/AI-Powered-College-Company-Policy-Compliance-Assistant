package com.policyguardian.service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GroqService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final WebClient webClient;

    public GroqService() {
        this.webClient = WebClient.builder()
                .codecs(configurer ->
                        configurer.defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024))
                .build();
    }

    @PostConstruct
    public void checkApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("Groq API Key Loaded: [NOT FOUND]");
        } else {
            System.out.println("Groq API Key Loaded: " + apiKey.substring(0, Math.min(apiKey.length(), 10)) + "...");
        }
        System.out.println("Groq Model Configured: " + model);
    }

    // ── Custom Rate Limit Exception ──────────────────────────────────────────
    public static class RateLimitException extends RuntimeException {
        public RateLimitException(String message) {
            super(message);
        }
    }

    // ── Groq API Request/Response DTOs ───────────────────────────────────────
    private record ResponseFormat(String type) {}
    private record GroqMessage(String role, String content) {}
    private record GroqRequest(
            String model,
            List<GroqMessage> messages,
            ResponseFormat response_format,
            double temperature
    ) {}

    private record Choice(int index, GroqMessage message, String finish_reason) {}
    private record Usage(int prompt_tokens, int completion_tokens, int total_tokens) {}
    private record GroqResponse(
            String id,
            String object,
            long created,
            String model,
            List<Choice> choices,
            Usage usage
    ) {}

    // ── Public AI Generation API ─────────────────────────────────────────────

    /**
     * Send system instructions and user chat conversation to Groq for structured JSON completion.
     */
    public String generateContent(String systemInstruction, String userPrompt) {
        String url = "https://api.groq.com/openai/v1/chat/completions";

        GroqRequest request = new GroqRequest(
                model,
                List.of(
                        new GroqMessage("system", systemInstruction),
                        new GroqMessage("user", userPrompt)
                ),
                new ResponseFormat("json_object"),
                0.0
        );

        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                System.out.println("========================================================================");
                System.out.println(">>> Sending request to Groq API (generateContent) - Attempt " + (attempt + 1));
                System.out.println(">>> URL: " + url);
                System.out.println(">>> Model: " + model);
                System.out.println(">>> Request Content (System Instruction length): " + systemInstruction.length() + " chars");
                System.out.println(">>> Request Content (User Prompt length): " + userPrompt.length() + " chars");
                System.out.println("========================================================================");

                GroqResponse response = webClient.post()
                        .uri(url)
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(request)
                        .retrieve()
                        .bodyToMono(GroqResponse.class)
                        .timeout(Duration.ofSeconds(30)) // Request timeout parameter
                        .block();

                if (response != null && response.choices() != null && !response.choices().isEmpty()) {
                    Choice choice = response.choices().get(0);
                    if (choice.message() != null && choice.message().content() != null) {
                        String content = choice.message().content();
                        System.out.println("<<< Groq API Response Received Successfully.");
                        if (response.usage() != null) {
                            System.out.println("<<< Usage -> Prompt Tokens: " + response.usage().prompt_tokens() + 
                                               ", Completion Tokens: " + response.usage().completion_tokens() + 
                                               ", Total Tokens: " + response.usage().total_tokens());
                        }
                        return content;
                    }
                }
                throw new RuntimeException("Empty response choices from Groq API");
            } catch (WebClientResponseException e) {
                int status = e.getStatusCode().value();
                System.err.printf("❌ Groq API HTTP Error - Status: %d, Response Body: %s%n", status, e.getResponseBodyAsString());
                
                // Retry for 429 and 503
                if ((status == 429 || status == 503) && attempt < maxRetries - 1) {
                    try {
                        long waitMs = (status == 429) ? getWaitTime429(e, attempt) : getWaitTime503(attempt);
                        System.err.printf("⚠️ Temporary status %d during generateContent (attempt %d). Retrying after waiting %.1f s...%n", 
                                status, attempt + 1, waitMs / 1000.0);
                        Thread.sleep(waitMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during backoff retry", ie);
                    }
                    continue;
                }
                handleException(e);
            } catch (Exception e) {
                System.err.println("❌ Groq Request Failed with Exception: " + e.getMessage());
                throw e;
            }
        }
        throw new RuntimeException("Failed to retrieve generation response from Groq API after retries.");
    }

    private long getWaitTime429(WebClientResponseException e, int attempt) {
        String retryAfterHeader = e.getHeaders().getFirst("Retry-After");
        if (retryAfterHeader != null) {
            try {
                return Long.parseLong(retryAfterHeader.trim()) * 1000L;
            } catch (NumberFormatException nfe) {
                // Ignore and use backoff
            }
        }
        // Exponential backoff starting at 2 seconds
        return 2000L * (long) Math.pow(2, attempt);
    }

    private long getWaitTime503(int attempt) {
        // Exponential backoff starting at 2 seconds
        return 2000L * (long) Math.pow(2, attempt);
    }

    private void handleException(WebClientResponseException e) {
        System.err.println("========================================================================");
        System.err.println("❌ Groq API Detailed Exception Report");
        System.err.println("Status  : " + e.getStatusCode());
        System.err.println("Headers : " + e.getHeaders());
        System.err.println("Response: " + e.getResponseBodyAsString());
        System.err.println("========================================================================");

        int status = e.getStatusCode().value();
        String responseBody = e.getResponseBodyAsString();
        switch (status) {
            case 401 -> throw new RuntimeException("Unauthorized: Invalid Groq API Key. " + responseBody);
            case 403 -> throw new RuntimeException("Forbidden: Access Denied to Groq resource. " + responseBody);
            case 404 -> throw new RuntimeException("Model Not Found: The configured model is invalid or unavailable. " + responseBody);
            case 429 -> throw new RateLimitException("Groq API Rate Limit Exceeded: " + responseBody);
            case 500 -> throw new RuntimeException("Internal Server Error: Groq service encountered an error. " + responseBody);
            case 503 -> throw new RuntimeException("Service Unavailable: Groq service is temporarily offline. " + responseBody);
            default -> throw new RuntimeException("HTTP Error " + status + " from Groq API: " + responseBody);
        }
    }

    // ── Placeholder Embeddings For Future Integration ───────────────────────

    /**
     * Placeholder to return a dummy embedding of 1536 dimensions.
     * Since Groq Chat Completions API does not natively support embeddings, this local method returns
     * a vector of 1536 zeros, enabling local fallback database security filtering to function perfectly.
     * 
     * ### Recommended Free Local Embedding Solution for Future Integration:
     * 1. **Hugging Face Sentence Transformers (Local & Offline)**:
     *    - **Recommended Model**: `sentence-transformers/all-MiniLM-L6-v2` (dimension 384) or `all-mpnet-base-v2` (dimension 768).
     *    - **Integration Library**: Use **LangChain4j** or **Deep Java Library (DJL)** with ONNX Runtime to load the model file directly in Java.
     *    - **Benefits**: 100% free, runs locally inside the Spring Boot process (no network latency/costs), robust semantic capability, highly secure.
     * 
     * 2. **Hugging Face Inference API (Cloud)**:
     *    - **Recommended Model**: `sentence-transformers/all-MiniLM-L6-v2`.
     *    - **Integration**: Send HTTP REST requests via Spring WebClient.
     *    - **Benefits**: Quick setup, free public cloud tier.
     */
    public List<Double> getEmbedding(String text) {
        System.out.println(">>> GroqService: Returning 1536-dimensional mock embedding of zeros for: '" + 
                           text.substring(0, Math.min(text.length(), 30)) + "...'");
        return Collections.nCopies(1536, 0.0);
    }

    /**
     * Batch mock embedding generator.
     */
    public List<List<Double>> getEmbeddings(List<String> texts) {
        System.out.println(">>> GroqService: Returning batch mock embeddings of zeros for " + texts.size() + " texts.");
        List<List<Double>> result = new ArrayList<>();
        for (int i = 0; i < texts.size(); i++) {
            result.add(getEmbedding(texts.get(i)));
        }
        return result;
    }
}
