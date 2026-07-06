package com.policyguardian.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CitationDTO(
    @JsonProperty("doc_id") String docId,
    @JsonProperty("doc_name") String docName,
    String section,
    String excerpt
) {}
