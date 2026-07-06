package com.policyguardian.config;

import com.policyguardian.dto.TextChunk;
import com.policyguardian.model.PolicyDocument;
import com.policyguardian.repository.PolicyDocumentRepository;
import com.policyguardian.service.ChromaService;
import com.policyguardian.service.DocumentParserService;
import com.policyguardian.service.GroqService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Order(10) // Run after database schema initializers
public class VectorStoreInitializer implements CommandLineRunner {

    @Autowired
    private PolicyDocumentRepository policyDocumentRepository;

    @Autowired
    private DocumentParserService documentParserService;

    @Autowired
    private GroqService groqService;

    @Autowired
    private ChromaService chromaService;

    private static final String UPLOAD_DIR = "uploads";
    private static final String CHROMA_COLLECTION = "policy_documents";

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔄 Syncing vector store with database entries...");

        // Trigger fallback check
        String collectionId = chromaService.getOrCreateCollection(CHROMA_COLLECTION);

        if (!collectionId.startsWith("in_memory_collection_")) {
            System.out.println("ℹ️ Persistent vector store ChromaDB is online. Skipping local re-indexing.");
            return;
        }

        List<PolicyDocument> documents = policyDocumentRepository.findAll();
        if (documents.isEmpty()) {
            System.out.println("ℹ️ No documents found in database to load.");
            return;
        }

        System.out.println("ℹ️ Offline mode active. Re-indexing " + documents.size() + " documents from database into Java in-memory store...");

        for (PolicyDocument doc : documents) {
            Path filePath = Paths.get(UPLOAD_DIR).resolve("doc_" + doc.getId() + "." + doc.getDocType().toLowerCase());
            File file = filePath.toFile();

            if (!file.exists()) {
                System.err.println("⚠️ Document file not found on disk: " + file.getAbsolutePath());
                continue;
            }

            System.out.println("🔄 Parsing and indexing document: " + doc.getName());
            try (InputStream inputStream = new FileInputStream(file)) {
                String textContent;
                if ("PDF".equalsIgnoreCase(doc.getDocType())) {
                    textContent = documentParserService.parsePdf(inputStream);
                } else if ("DOCX".equalsIgnoreCase(doc.getDocType())) {
                    textContent = documentParserService.parseDocx(inputStream);
                } else {
                    continue;
                }

                List<TextChunk> chunks = documentParserService.chunkText(textContent, 500, 50);
                if (!chunks.isEmpty()) {
                    List<String> chunkContents = new ArrayList<>();
                    for (TextChunk chunk : chunks) {
                        chunkContents.add(chunk.content());
                    }

                    List<List<Double>> embeddings = groqService.getEmbeddings(chunkContents);

                    List<String> ids = new ArrayList<>();
                    List<Map<String, Object>> metadatas = new ArrayList<>();

                    for (int i = 0; i < chunks.size(); i++) {
                        TextChunk chunk = chunks.get(i);
                        ids.add("doc_" + doc.getId() + "_chunk_" + chunk.chunkIndex());
                        metadatas.add(Map.of(
                                "doc_id", doc.getId(),
                                "doc_name", doc.getName(),
                                "section", chunk.sectionName(),
                                "role_scope", doc.getRoleScope(),
                                "department", doc.getDepartmentScope()
                        ));
                    }

                    chromaService.addChunks(collectionId, ids, embeddings, metadatas, chunkContents);
                }
            } catch (Exception e) {
                System.err.println("❌ Failed to index document " + doc.getName() + ": " + e.getMessage());
            }
        }
        System.out.println("✅ Syncing vector store completed.");
    }
}
