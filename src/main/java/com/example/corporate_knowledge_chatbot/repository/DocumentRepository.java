package com.example.corporate_knowledge_chatbot.repository;

import com.example.corporate_knowledge_chatbot.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findAllByOrderByUploadedAtDesc();
}
