package com.policyguardian.controller;

import com.policyguardian.model.PolicyDocument;
import com.policyguardian.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private PolicyService policyService;

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam("departmentScope") String departmentScope,
            @RequestParam("roleScope") String roleScope,
            Authentication authentication) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "File is empty"));
        }

        try {
            String uploadedBy = authentication.getName();
            PolicyDocument document = policyService.uploadAndIndexDocument(
                    file,
                    departmentScope,
                    roleScope,
                    uploadedBy
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(document);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to upload document: " + ex.getMessage()));
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<PolicyDocument>> getAllDocuments() {
        List<PolicyDocument> documents = policyService.getAllDocuments();
        return ResponseEntity.ok(documents);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> deleteDocument(@PathVariable("id") Long id) {
        try {
            policyService.deleteDocument(id);
            return ResponseEntity.ok(Map.of("message", "Document deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to delete document: " + ex.getMessage()));
        }
    }
}
