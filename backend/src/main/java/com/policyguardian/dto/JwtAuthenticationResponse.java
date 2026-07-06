package com.policyguardian.dto;

public record JwtAuthenticationResponse(
    String token,
    String tokenType,
    String username,
    String email,
    String role,
    String department
) {
    public JwtAuthenticationResponse(String token, String username, String email, String role, String department) {
        this(token, "Bearer", username, email, role, department);
    }
}
