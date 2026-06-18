package com.example.corporate_knowledge_chatbot.service;

import com.example.corporate_knowledge_chatbot.dto.ChatDto;
import com.example.corporate_knowledge_chatbot.entity.ChatHistory;
import com.example.corporate_knowledge_chatbot.entity.DocumentChunk;
import com.example.corporate_knowledge_chatbot.entity.User;
import com.example.corporate_knowledge_chatbot.exception.ChatbotException;
import com.example.corporate_knowledge_chatbot.repository.ChatHistoryRepository;
import com.example.corporate_knowledge_chatbot.repository.DocumentChunkRepository;
import com.example.corporate_knowledge_chatbot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final DocumentChunkRepository chunkRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final UserRepository userRepository;
    private final GeminiService geminiService;

    /**
     * Full RAG pipeline:
     * 1. Extract keywords from question
     * 2. Search relevant chunks in MySQL
     * 3. Build context string
     * 4. Send to Gemini
     * 5. Save chat history
     * 6. Return response with source document
     */
    @Transactional
    public ChatDto.ChatResponse chat(String question) {
        User currentUser = getCurrentUser();

        log.info("Chat request from '{}': {}", currentUser.getEmail(), question);

        // Step 1: Find relevant chunks using keyword search
        List<DocumentChunk> relevantChunks = findRelevantChunks(question);

        if (relevantChunks.isEmpty()) {
            String noContextAnswer = "No relevant documents found in the knowledge base. " +
                    "Please ask your admin to upload relevant company documents.";

            saveChatHistory(currentUser, question, noContextAnswer, null);

            return ChatDto.ChatResponse.builder()
                    .question(question)
                    .answer(noContextAnswer)
                    .sourceDocument(null)
                    .build();
        }

        // Step 2: Build context from top matching chunks
        String context = buildContext(relevantChunks);

        // Step 3: Identify source document for citation
        String sourceDocument = relevantChunks.get(0).getDocument().getFileName();

        // Step 4: Get answer from Gemini
        String answer = geminiService.generateAnswer(context, question);

        // Step 5: Save to chat history
        ChatHistory saved = saveChatHistory(currentUser, question, answer, sourceDocument);

        return ChatDto.ChatResponse.builder()
                .question(question)
                .answer(answer)
                .sourceDocument(sourceDocument)
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * Search chunks using multiple keywords extracted from the question.
     * Deduplicates results while preserving insertion order.
     */
    private List<DocumentChunk> findRelevantChunks(String question) {
        // Extract meaningful keywords (ignore stop words)
        List<String> keywords = extractKeywords(question);

        Set<DocumentChunk> uniqueChunks = new LinkedHashSet<>();

        for (String keyword : keywords) {
            List<DocumentChunk> found = chunkRepository.findTopChunksByKeyword(keyword);
            uniqueChunks.addAll(found);
            if (uniqueChunks.size() >= 5) break; // cap at 5 unique chunks
        }

        List<DocumentChunk> result = uniqueChunks.stream().limit(5).collect(Collectors.toList());
        log.debug("Found {} relevant chunks for question: {}", result.size(), question);
        return result;
    }

    /**
     * Extract meaningful keywords by removing common stop words.
     */
    private List<String> extractKeywords(String question) {
        Set<String> stopWords = Set.of(
                "how", "many", "what", "is", "are", "the", "a", "an", "in", "of",
                "for", "to", "do", "does", "can", "i", "we", "our", "my", "their",
                "have", "has", "will", "be", "been", "was", "were", "get", "please",
                "tell", "me", "us", "about", "which", "who", "when", "where", "why"
        );

        return Arrays.stream(question.toLowerCase().split("\\s+"))
                .map(w -> w.replaceAll("[^a-zA-Z0-9]", ""))
                .filter(w -> w.length() > 2 && !stopWords.contains(w))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Combine top chunks into a single context string for the prompt.
     */
    private String buildContext(List<DocumentChunk> chunks) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            context.append("[Excerpt ").append(i + 1)
                    .append(" from ").append(chunk.getDocument().getFileName())
                    .append(", Chunk #").append(chunk.getChunkNumber()).append("]\n");
            context.append(chunk.getChunkText()).append("\n\n");
        }
        return context.toString();
    }

    private ChatHistory saveChatHistory(User user, String question, String answer, String source) {
        ChatHistory history = ChatHistory.builder()
                .user(user)
                .question(question)
                .answer(answer)
                .sourceDocument(source)
                .build();
        return chatHistoryRepository.save(history);
    }

    /**
     * Get chat history for the currently authenticated user.
     */
    public List<ChatDto.ChatHistoryResponse> getChatHistory() {
        User currentUser = getCurrentUser();
        return chatHistoryRepository
                .findTop20ByUserIdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(h -> ChatDto.ChatHistoryResponse.builder()
                        .id(h.getId())
                        .question(h.getQuestion())
                        .answer(h.getAnswer())
                        .sourceDocument(h.getSourceDocument())
                        .createdAt(h.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get the currently authenticated user from the security context.
     */
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ChatbotException.UnauthorizedException(
                        "User not found in security context"));
    }
}
