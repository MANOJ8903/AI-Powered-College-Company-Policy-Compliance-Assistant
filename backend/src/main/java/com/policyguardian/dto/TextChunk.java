package com.policyguardian.dto;

public record TextChunk(
    String content,
    int chunkIndex,
    String sectionName
) {}
