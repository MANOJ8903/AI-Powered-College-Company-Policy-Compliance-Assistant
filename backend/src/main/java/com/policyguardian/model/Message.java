package com.policyguardian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @NotBlank
    @Column(nullable = false)
    private String sender; // "USER" or "ASSISTANT"

    @NotBlank
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Lob
    @Column(name = "citations_json", columnDefinition = "TEXT")
    private String citationsJson;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "escalate_flag", nullable = false)
    private Boolean escalateFlag;

    @Column(name = "insufficient_context", nullable = false)
    private Boolean insufficientContext;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
