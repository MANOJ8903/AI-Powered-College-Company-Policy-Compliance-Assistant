package com.policyguardian.controller;

import com.policyguardian.model.PolicyDocument;
import com.policyguardian.model.User;
import com.policyguardian.repository.MessageRepository;
import com.policyguardian.repository.UserRepository;
import com.policyguardian.service.PolicyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private PolicyService policyService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/analytics/unanswered")
    public ResponseEntity<List<Object[]>> getUnansweredQuestions() {
        List<Object[]> stats = messageRepository.findMostFrequentUnansweredQuestions();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/analytics/usage")
    public ResponseEntity<List<Object[]>> getUsageStats() {
        List<Object[]> stats = messageRepository.findUsageOverTime();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/documents")
    public ResponseEntity<List<PolicyDocument>> getDocuments() {
        List<PolicyDocument> docs = policyService.getAllDocuments();
        return ResponseEntity.ok(docs);
    }

    @DeleteMapping("/documents/{id}")
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

    @GetMapping("/users")
    public ResponseEntity<List<User>> getUsers() {
        List<User> users = userRepository.findAll();
        // Remove password hashes from response for security
        for (User u : users) {
            u.setPassword("[PROTECTED]");
        }
        return ResponseEntity.ok(users);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<?> updateUserRole(@PathVariable("id") Long id, @RequestBody Map<String, String> body) {
        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            String roleStr = body.get("role");
            user.setRole(com.policyguardian.model.Role.valueOf(roleStr.toUpperCase()));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "User role updated successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid role option."));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to update role: " + ex.getMessage()));
        }
    }
}
