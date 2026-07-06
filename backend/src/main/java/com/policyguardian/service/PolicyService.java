package com.policyguardian.service;

import com.policyguardian.dto.TextChunk;
import com.policyguardian.model.PolicyDocument;
import com.policyguardian.repository.PolicyDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PolicyService {

    @Autowired
    private PolicyDocumentRepository policyDocumentRepository;

    @Autowired
    private DocumentParserService documentParserService;

    @Autowired
    private GroqService groqService;

    @Autowired
    private ChromaService chromaService;

    @Value("${policy.chunk.size:500}")
    private int chunkSize;

    @Value("${policy.chunk.overlap:50}")
    private int chunkOverlap;

    private static final String UPLOAD_DIR = "uploads";
    private static final String CHROMA_COLLECTION = "policy_documents";

    @Transactional
    public PolicyDocument uploadAndIndexDocument(MultipartFile file,
                                                 String departmentScope,
                                                 String roleScope,
                                                 String uploadedBy) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new IllegalArgumentException("Invalid file name");
        }
        
        String fileExtension = getFileExtension(originalFilename);
        
        // Extract text directly from the MultipartFile inputStream
        String textContent;
        try (InputStream parseStream = file.getInputStream()) {
            if ("pdf".equalsIgnoreCase(fileExtension)) {
                textContent = documentParserService.parsePdf(parseStream);
            } else if ("docx".equalsIgnoreCase(fileExtension)) {
                textContent = documentParserService.parseDocx(parseStream);
            } else {
                throw new IllegalArgumentException("Unsupported file type: only PDF and DOCX are allowed.");
            }
        }

        // Save metadata record first to generate the auto-increment ID
        PolicyDocument policyDoc = PolicyDocument.builder()
                .name(originalFilename)
                .docType(fileExtension.toUpperCase())
                .departmentScope(departmentScope)
                .roleScope(roleScope)
                .uploadedBy(uploadedBy)
                .chromaCollectionRef(CHROMA_COLLECTION)
                .build();

        policyDoc = policyDocumentRepository.save(policyDoc);

        // Store a copy on disk using the convention doc_[id].[extension]
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        String uniqueFilename = "doc_" + policyDoc.getId() + "." + fileExtension;
        Path targetLocation = uploadPath.resolve(uniqueFilename);
        if (!Files.exists(targetLocation)) {
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation);
            }
        }

        // Chunk text using configured parameters
        List<TextChunk> chunks = documentParserService.chunkText(textContent, chunkSize, chunkOverlap);

        if (!chunks.isEmpty()) {
            List<String> chunkContents = new ArrayList<>();
            for (TextChunk chunk : chunks) {
                chunkContents.add(chunk.content());
            }

            // Calls GroqService which generates mock dummy embeddings of zeros (bypassing external embeddings API)
            List<List<Double>> embeddings = groqService.getEmbeddings(chunkContents);

            String collectionId = chromaService.getOrCreateCollection(CHROMA_COLLECTION);
            
            List<String> ids = new ArrayList<>();
            List<Map<String, Object>> metadatas = new ArrayList<>();
            
            for (int i = 0; i < chunks.size(); i++) {
                TextChunk chunk = chunks.get(i);
                ids.add("doc_" + policyDoc.getId() + "_chunk_" + chunk.chunkIndex());
                metadatas.add(Map.of(
                        "doc_id", policyDoc.getId(),
                        "doc_name", policyDoc.getName(),
                        "section", chunk.sectionName(),
                        "role_scope", roleScope,
                        "department", departmentScope
                ));
            }

            chromaService.addChunks(collectionId, ids, embeddings, metadatas, chunkContents);
        }

        return policyDoc;
    }

    /**
     * Transactionally delete a document record from MySQL, delete its vectors from ChromaDB, and remove the file from local storage.
     */
    @Transactional
    public void deleteDocument(Long id) throws IOException {
        PolicyDocument doc = policyDocumentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Document not found"));

        // Delete from ChromaDB vector store
        String collectionId = chromaService.getOrCreateCollection(CHROMA_COLLECTION);
        chromaService.deleteChunksByDocId(collectionId, doc.getId());

        // Delete metadata record from MySQL
        policyDocumentRepository.delete(doc);

        // Delete from local disk
        Path filePath = Paths.get(UPLOAD_DIR).resolve("doc_" + doc.getId() + "." + doc.getDocType().toLowerCase());
        Files.deleteIfExists(filePath);
    }

    public List<PolicyDocument> getAllDocuments() {
        return policyDocumentRepository.findAll();
    }

    private String getFileExtension(String filename) {
        int lastIndex = filename.lastIndexOf('.');
        if (lastIndex == -1) {
            return "";
        }
        return filename.substring(lastIndex + 1);
    }
}



