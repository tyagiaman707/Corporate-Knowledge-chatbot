package com.example.corporate_knowledge_chatbot.repository;

import com.example.corporate_knowledge_chatbot.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    List<DocumentChunk> findByDocumentId(Long documentId);
    @Query(value = """
            SELECT dc.* FROM document_chunks dc
            INNER JOIN documents d ON dc.document_id = d.id
            WHERE dc.chunk_text LIKE %:keyword%
            ORDER BY d.uploaded_at DESC
            LIMIT 5
            """, nativeQuery = true)
    List<DocumentChunk> findTopChunksByKeyword(@Param("keyword") String keyword);

    @Query(value = """
            SELECT dc.* FROM document_chunks dc
            WHERE MATCH(dc.chunk_text) AGAINST (:searchQuery IN NATURAL LANGUAGE MODE)
            LIMIT 5
            """, nativeQuery = true)
    List<DocumentChunk> findChunksByFullText(@Param("searchQuery") String searchQuery);
}