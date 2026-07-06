package com.policyguardian.dto;

public record ChromaSearchResult(
    String id,
    String document,
    Double distance,
    Long docId,
    String docName,
    String section,
    String roleScope,
    String department
) {}
