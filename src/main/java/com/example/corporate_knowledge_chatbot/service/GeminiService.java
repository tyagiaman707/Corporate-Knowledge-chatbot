package com.example.corporate_knowledge_chatbot.service;

import com.example.corporate_knowledge_chatbot.exception.ChatbotException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${openai.api.key}")
    private String openAiApiKey;

    @Value("${openai.api.url}")
    private String openAiApiUrl;

    @Value("${openai.model}")
    private String openAiModel;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateAnswer(String context, String question) {
        String prompt = buildPrompt(context, question);

        try {
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openAiApiKey);  // Authorization: Bearer sk-...

            Map<String, Object> requestBody = Map.of(
                    "model", openAiModel,
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are a helpful corporate knowledge assistant. " +
                                            "Answer questions using ONLY the provided company document context. " +
                                            "If the answer is not in the context, say: " +
                                            "'I could not find relevant information in the uploaded documents.' " +
                                            "Be concise and professional."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "temperature", 0.2,
                    "max_tokens", 1024
            );

            String requestJson = objectMapper.writeValueAsString(requestBody);
            HttpEntity<String> entity = new HttpEntity<>(requestJson, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    openAiApiUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseOpenAiResponse(response.getBody());
            } else {
                throw new ChatbotException.GeminiApiException(
                        "OpenAI returned unexpected status: " + response.getStatusCode());
            }

        } catch (ChatbotException.GeminiApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage(), e);
            throw new ChatbotException.GeminiApiException(
                    "Failed to get answer from AI service. Please try again.", e);
        }
    }

    private String buildPrompt(String context, String question) {
        return """
                CONTEXT FROM COMPANY DOCUMENTS:
                %s
                ---
                EMPLOYEE QUESTION:
                %s
                ---
                ANSWER:
                """.formatted(context, question);
    }

    private String parseOpenAiResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                return choices.get(0)
                        .path("message")
                        .path("content")
                        .asText("No answer generated.");
            }

            log.warn("Unexpected OpenAI response structure: {}", responseBody);
            return "I was unable to process the AI response. Please try again.";

        } catch (Exception e) {
            log.error("Failed to parse OpenAI response: {}", e.getMessage());
            throw new ChatbotException.GeminiApiException("Failed to parse OpenAI response", e);
        }
    }
}