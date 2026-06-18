package com.example.corporate_knowledge_chatbot.controller;
import com.example.corporate_knowledge_chatbot.dto.ApiResponse;
import com.example.corporate_knowledge_chatbot.dto.ChatDto;
import com.example.corporate_knowledge_chatbot.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * POST /api/chat/ask
     * Employee asks a question — returns AI-generated answer with source document
     *
     * Headers: Authorization: Bearer <token>
     * Body:    { "question": "How many casual leaves are allowed?" }
     */
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<ChatDto.ChatResponse>> askQuestion(
            @Valid @RequestBody ChatDto.ChatRequest request) {

        ChatDto.ChatResponse response = chatService.chat(request.getQuestion());
        return ResponseEntity.ok(ApiResponse.success(response, "Answer generated successfully"));
    }

    /**
     * GET /api/chat/history
     * Retrieve the last 20 chat messages for the current user
     *
     * Headers: Authorization: Bearer <token>
     */
    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ChatDto.ChatHistoryResponse>>> getChatHistory() {
        List<ChatDto.ChatHistoryResponse> history = chatService.getChatHistory();
        return ResponseEntity.ok(ApiResponse.success(history, "Chat history fetched successfully"));
    }
}