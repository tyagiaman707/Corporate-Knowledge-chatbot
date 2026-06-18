package com.example.corporate_knowledge_chatbot.repository;

import com.example.corporate_knowledge_chatbot.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    List<ChatHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ChatHistory> findTop20ByUserIdOrderByCreatedAtDesc(Long userId);
}