package com.example.corporate_knowledge_chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

public class ChatDto {

    // ── Chat Request ──────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRequest {
        @NotBlank(message = "Question cannot be empty")
        private String question;
    }

    // ── Chat Response ─────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatResponse {
        private String question;
        private String answer;
        private String sourceDocument;
        private LocalDateTime createdAt;
    }

    // ── Chat History Item ─────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatHistoryResponse {
        private Long id;
        private String question;
        private String answer;
        private String sourceDocument;
        private LocalDateTime createdAt;
    }
}