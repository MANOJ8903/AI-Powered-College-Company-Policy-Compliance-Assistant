package com.policyguardian.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "doc_name", nullable = false)
    private String name;

    @NotBlank
    @Column(name = "doc_type", nullable = false)
    private String docType; // e.g. "PDF", "DOCX"

    @NotBlank
    @Column(name = "role_scope", nullable = false)
    private String roleScope;

    @NotBlank
    @Column(name = "department_scope", nullable = false)
    private String departmentScope;

    @NotBlank
    @Column(name = "uploaded_by", nullable = false)
    private String uploadedBy;

    @NotBlank
    @Column(name = "chroma_collection_ref", nullable = false)
    private String chromaCollectionRef; // collection name reference

    @Column(name = "uploaded_at", insertable = false, updatable = false)
    private LocalDateTime uploadTime;
}
