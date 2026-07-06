package com.policyguardian.dto;

import java.util.List;

public record ChatResponse(
    String answer,
    List<CitationDTO> citations,
    String confidence, // "high", "medium", "low"
    Boolean insufficientContext,
    Boolean escalate,
    String escalationContact,
    List<String> followUpSuggestions,
    String sessionId
) {}
