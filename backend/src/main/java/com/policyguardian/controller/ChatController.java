package com.policyguardian.controller;

import com.policyguardian.dto.ChatRequest;
import com.policyguardian.dto.ChatResponse;
import com.policyguardian.model.User;
import com.policyguardian.service.ChatService;
import com.policyguardian.service.GroqService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

@RestController
@RequestMapping("/api")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> askPolicyQuestion(
            @Valid @RequestBody ChatRequest chatRequest,
            Authentication authentication) {

        User user = (User) authentication.getPrincipal();
        ChatResponse response = chatService.processQuery(
                chatRequest.question(),
                chatRequest.sessionId(),
                user
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Catches Groq rate-limit exhaustion.
     * Returns HTTP 200 with a friendly chat bubble message.
     */
    @ExceptionHandler(GroqService.RateLimitException.class)
    public ResponseEntity<ChatResponse> handleRateLimit(GroqService.RateLimitException ex) {
        ChatResponse errorResponse = new ChatResponse(
                "⏳ " + ex.getMessage(),
                new ArrayList<>(),
                "low",
                true,
                false,
                null,
                new ArrayList<>(),
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
    }

    /**
     * Generic fallback for all other unexpected errors.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ChatResponse> handleException(Exception ex) {
        System.err.println("❌ Unexpected chat error: " + ex.getClass().getSimpleName() + " — " + ex.getMessage());
        ChatResponse errorResponse = new ChatResponse(
                "A temporary error occurred. Please try again in a moment.",
                new ArrayList<>(),
                "low",
                true,
                false,
                null,
                new ArrayList<>(),
                null
        );
        return ResponseEntity.status(HttpStatus.OK).body(errorResponse);
    }
}
