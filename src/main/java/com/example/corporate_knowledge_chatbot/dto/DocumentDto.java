package com.example.corporate_knowledge_chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class DocumentDto {

    // ── Document Upload Response ───────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentResponse {
        private Long id;
        private String fileName;
        private String uploadedBy;
        private LocalDateTime uploadedAt;
        private Integer totalChunks;
        private String message;
    }
}