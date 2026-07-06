package com.policyguardian.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record PolicyAnswerDTO(
    String answer,
    List<CitationDTO> citations,
    String confidence,
    
    @JsonProperty("insufficient_context")
    boolean insufficientContext,
    
    boolean escalate,
    
    @JsonProperty("escalation_contact")
    String escalationContact,
    
    @JsonProperty("follow_up_suggestions")
    List<String> followUpSuggestions
) {}
