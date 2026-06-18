package com.example.corporate_knowledge_chatbot.controller;

import com.example.corporate_knowledge_chatbot.dto.ApiResponse;
import com.example.corporate_knowledge_chatbot.dto.DocumentDto;
import com.example.corporate_knowledge_chatbot.entity.Document;
import com.example.corporate_knowledge_chatbot.entity.User;
import com.example.corporate_knowledge_chatbot.exception.ChatbotException;
import com.example.corporate_knowledge_chatbot.repository.UserRepository;
import com.example.corporate_knowledge_chatbot.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final PdfService pdfService;
    private final UserRepository userRepository;

    /**
     * POST /api/admin/upload
     * Upload a PDF document to the knowledge base
     *
     * Usage: multipart/form-data with field "file"
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<DocumentDto.DocumentResponse>> uploadPdf(
            @RequestParam("file") MultipartFile file) {

        User admin = getCurrentAdmin();
        Document document = pdfService.processPdf(file, admin);

        DocumentDto.DocumentResponse response = toDocumentResponse(document, "PDF uploaded and processed successfully");
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    /**
     * GET /api/admin/documents
     * List all uploaded documents
     */
    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<DocumentDto.DocumentResponse>>> getAllDocuments() {
        List<DocumentDto.DocumentResponse> docs = pdfService.getAllDocuments()
                .stream()
                .map(d -> toDocumentResponse(d, null))
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(docs, "Documents fetched successfully"));
    }

    /**
     * DELETE /api/admin/documents/{id}
     * Delete a document and all its chunks from the knowledge base
     */
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        pdfService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }

    // ─── Helpers ──────────────────────────────────────────────

    private User getCurrentAdmin() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ChatbotException.UnauthorizedException("Admin not found"));
    }

    private DocumentDto.DocumentResponse toDocumentResponse(Document doc, String message) {
        return DocumentDto.DocumentResponse.builder()
                .id(doc.getId())
                .fileName(doc.getFileName())
                .uploadedBy(doc.getUploadedBy().getName())
                .uploadedAt(doc.getUploadedAt())
                .totalChunks(doc.getTotalChunks())
                .message(message)
                .build();
    }
}