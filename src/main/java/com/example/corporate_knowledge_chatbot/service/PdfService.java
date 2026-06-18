package com.example.corporate_knowledge_chatbot.service;

import com.example.corporate_knowledge_chatbot.entity.Document;
import com.example.corporate_knowledge_chatbot.entity.DocumentChunk;
import com.example.corporate_knowledge_chatbot.entity.User;
import com.example.corporate_knowledge_chatbot.exception.ChatbotException;
import com.example.corporate_knowledge_chatbot.repository.DocumentChunkRepository;
import com.example.corporate_knowledge_chatbot.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;

    @Value("${app.chunk.max-words:400}")
    private int maxWordsPerChunk;

    @Value("${app.chunk.min-words:100}")
    private int minWordsPerChunk;

    /**
     * Complete pipeline: upload → extract text → chunk → save to DB
     */
    @Transactional
    public Document processPdf(MultipartFile file, User uploadedBy) {
        validatePdfFile(file);

        // Step 1: Extract raw text from PDF
        String rawText = extractTextFromPdf(file);

        if (rawText == null || rawText.isBlank()) {
            throw new ChatbotException.BadRequestException(
                    "Could not extract text from PDF. The file may be scanned/image-based.");
        }

        // Step 2: Save document metadata
        Document document = Document.builder()
                .fileName(file.getOriginalFilename())
                .uploadedBy(uploadedBy)
                .build();
        document = documentRepository.save(document);

        // Step 3: Split text into chunks and save
        List<String> chunks = splitIntoChunks(rawText);
        saveChunks(document, chunks);

        // Update total chunk count
        document.setTotalChunks(chunks.size());
        document = documentRepository.save(document);

        log.info("PDF processed: '{}' → {} chunks created", file.getOriginalFilename(), chunks.size());
        return document;
    }

    /**
     * Extract all text from a PDF using Apache PDFBox
     */
    private String extractTextFromPdf(MultipartFile file) {
        try (PDDocument pdDocument = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // maintains reading order
            String text = stripper.getText(pdDocument);
            log.debug("Extracted {} characters from PDF", text.length());
            return text;
        } catch (IOException e) {
            throw new ChatbotException.PdfProcessingException(
                    "Failed to read or extract text from PDF: " + e.getMessage(), e);
        }
    }

    /**
     * Split text into overlapping word-based chunks.
     * Uses a sliding window approach for context continuity.
     */
    List<String> splitIntoChunks(String text) {
        String[] words = text.split("\\s+");
        List<String> chunks = new ArrayList<>();

        int i = 0;
        while (i < words.length) {
            int end = Math.min(i + maxWordsPerChunk, words.length);
            StringBuilder chunk = new StringBuilder();

            for (int j = i; j < end; j++) {
                chunk.append(words[j]);
                if (j < end - 1) chunk.append(" ");
            }

            String chunkText = chunk.toString().trim();

            // Skip undersized trailing chunks — append to last chunk instead
            if (chunks.isEmpty() || chunkText.split("\\s+").length >= minWordsPerChunk) {
                chunks.add(chunkText);
            } else {
                String last = chunks.get(chunks.size() - 1);
                chunks.set(chunks.size() - 1, last + " " + chunkText);
            }

            // Move forward with slight overlap (90% stride) for context continuity
            i += (int) (maxWordsPerChunk * 0.9);
        }

        return chunks;
    }

    /**
     * Persist all chunks linked to the parent document
     */
    private void saveChunks(Document document, List<String> chunks) {
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            chunkEntities.add(DocumentChunk.builder()
                    .document(document)
                    .chunkNumber(i + 1)
                    .chunkText(chunks.get(i))
                    .build());
        }
        documentChunkRepository.saveAll(chunkEntities);
    }

    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ChatbotException.BadRequestException("No file provided");
        }

        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".pdf")) {
            throw new ChatbotException.BadRequestException("Only PDF files are accepted");
        }

        // 20 MB limit
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new ChatbotException.BadRequestException("File size must not exceed 20MB");
        }
    }

    /**
     * Retrieve all documents list
     */
    public List<Document> getAllDocuments() {
        return documentRepository.findAllByOrderByUploadedAtDesc();
    }

    /**
     * Delete a document and all its chunks (cascades automatically)
     */
    @Transactional
    public void deleteDocument(Long documentId) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ChatbotException.ResourceNotFoundException(
                        "Document not found with id: " + documentId));
        documentRepository.delete(doc);
        log.info("Document deleted: id={}, name={}", documentId, doc.getFileName());
    }
}