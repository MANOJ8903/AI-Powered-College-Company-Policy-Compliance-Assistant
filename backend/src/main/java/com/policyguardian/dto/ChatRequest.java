package com.policyguardian.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
    @NotBlank(message = "Question is required")
    String question,
    
    String sessionId // Optional; a new session will be generated if not provided
) {}
