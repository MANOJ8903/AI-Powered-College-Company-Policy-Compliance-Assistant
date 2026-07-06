package com.policyguardian.service;

import com.policyguardian.dto.ChromaSearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ChromaService {

    @Value("${chromadb.api.url}")
    private String chromaUrl;

    private final WebClient webClient;

    // High-fidelity local vector database fallback
    private static final List<MockChromaRecord> mockDb = new CopyOnWriteArrayList<>();
    private static boolean useFallbackMode = false;

    private record MockChromaRecord(
            String id,
            List<Double> embedding,
            Map<String, Object> metadata,
            String document
    ) {}

    public ChromaService() {
        this.webClient = WebClient.builder().build();
    }

    // Chroma REST API Request/Response DTOs
    private record CollectionRequest(String name, boolean get_or_create) {}
    private record CollectionResponse(String id, String name) {}
    private record AddRequest(
            List<String> ids,
            List<List<Double>> embeddings,
            List<Map<String, Object>> metadatas,
            List<String> documents
    ) {}
    private record QueryRequest(
            List<List<Double>> query_embeddings,
            int n_results,
            Map<String, Object> where
    ) {}
    private record QueryResponse(
            List<List<String>> ids,
            List<List<Double>> distances,
            List<List<Map<String, Object>>> metadatas,
            List<List<String>> documents
    ) {}

    /**
     * Retrieve or create a ChromaDB collection by name. Returns the collection UUID.
     */
    public String getOrCreateCollection(String name) {
        if (useFallbackMode) {
            return "in_memory_collection_" + name;
        }

        String url = chromaUrl + "/api/v1/collections";
        try {
            CollectionResponse response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new CollectionRequest(name, true))
                    .retrieve()
                    .bodyToMono(CollectionResponse.class)
                    .block();

            if (response != null) {
                return response.id();
            }
        } catch (Exception e) {
            System.err.println("âš ï¸ ChromaDB server is offline. Enabling Java in-memory fallback vector store...");
            useFallbackMode = true;
            return "in_memory_collection_" + name;
        }
        throw new RuntimeException("Failed to get or create collection: " + name);
    }

    /**
     * Add text chunks, embeddings, and metadata into a ChromaDB collection.
     */
    public void addChunks(String collectionId,
                          List<String> ids,
                          List<List<Double>> embeddings,
                          List<Map<String, Object>> metadatas,
                          List<String> documents) {
        if (useFallbackMode) {
            for (int i = 0; i < ids.size(); i++) {
                mockDb.add(new MockChromaRecord(
                        ids.get(i),
                        embeddings.get(i),
                        metadatas.get(i),
                        documents.get(i)
                ));
            }
            System.out.println("âœ… Ingested " + ids.size() + " chunks into the Java in-memory fallback vector store.");
            return;
        }

        String url = chromaUrl + "/api/v1/collections/" + collectionId + "/add";
        try {
            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(new AddRequest(ids, embeddings, metadatas, documents))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            System.err.println("âš ï¸ ChromaDB write failed. Switched to fallback store.");
            useFallbackMode = true;
            addChunks(collectionId, ids, embeddings, metadatas, documents);
        }
    }

    /**
     * Query a collection with a question embedding and apply user scope metadata filters.
     */
    public List<ChromaSearchResult> queryCollection(String collectionId,
                                                   List<Double> queryEmbedding,
                                                   int nResults,
                                                   String role,
                                                   String department,
                                                   String rawQuery) {
        if (useFallbackMode) {
            return performInMemoryQuery(queryEmbedding, nResults, role, department, rawQuery);
        }

        String url = chromaUrl + "/api/v1/collections/" + collectionId + "/query";

        // Build metadata filters based on user's authorized role and department scopes
        // ADMIN role bypasses scopes to allow querying all documents.
        Map<String, Object> where = Map.of();
        if (!"ADMIN".equalsIgnoreCase(role)) {
            where = Map.of(
                    "$and", List.of(
                            Map.of("role_scope", Map.of("$in", List.of("ALL", role))),
                            Map.of("department", Map.of("$in", List.of("ALL", department)))
                    )
            );
        }

        QueryRequest request = new QueryRequest(List.of(queryEmbedding), nResults, where);

        try {
            QueryResponse response = webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(QueryResponse.class)
                    .block();

            List<ChromaSearchResult> results = new ArrayList<>();
            if (response != null && response.ids() != null && !response.ids().isEmpty()) {
                List<String> ids = response.ids().get(0);
                List<Double> distances = response.distances().get(0);
                List<Map<String, Object>> metadatas = response.metadatas().get(0);
                List<String> documents = response.documents().get(0);

                for (int i = 0; i < ids.size(); i++) {
                    Map<String, Object> metadata = metadatas.get(i);
                    
                    Long docId = metadata.containsKey("doc_id") ? ((Number) metadata.get("doc_id")).longValue() : null;
                    String docName = (String) metadata.getOrDefault("doc_name", "Unknown");
                    String section = (String) metadata.getOrDefault("section", "General");
                    String roleScope = (String) metadata.getOrDefault("role_scope", "ALL");
                    String dept = (String) metadata.getOrDefault("department", "ALL");
                    double distance = distances.get(i);
                    if (rawQuery != null) {
                        distance = calculateKeywordDistance(documents.get(i), rawQuery);
                        if (distance >= 1.0) {
                            continue;
                        }
                    }

                    results.add(new ChromaSearchResult(
                            ids.get(i),
                            documents.get(i),
                            distance,
                            docId,
                            docName,
                            section,
                            roleScope,
                            dept
                    ));
                }
            }
            return results;
        } catch (Exception e) {
            System.err.println("âš ï¸ ChromaDB query failed. Executing query against the fallback store...");
            useFallbackMode = true;
            return performInMemoryQuery(queryEmbedding, nResults, role, department, rawQuery);
        }
    }

    /**
     * Delete all chunks associated with a specific document ID from ChromaDB.
     */
    public void deleteChunksByDocId(String collectionId, Long docId) {
        if (useFallbackMode) {
            mockDb.removeIf(record -> {
                Long rDocId = record.metadata() != null && record.metadata().containsKey("doc_id") 
                        ? ((Number) record.metadata().get("doc_id")).longValue() : null;
                return Objects.equals(rDocId, docId);
            });
            System.out.println("âœ… Deleted chunks for doc ID " + docId + " from Java in-memory store.");
            return;
        }

        String url = chromaUrl + "/api/v1/collections/" + collectionId + "/delete";
        Map<String, Object> body = Map.of("where", Map.of("doc_id", docId));

        try {
            webClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            System.err.println("âš ï¸ ChromaDB delete failed. Switched to fallback store.");
            useFallbackMode = true;
            deleteChunksByDocId(collectionId, docId);
        }
    }

    // In-memory query implementation using Cosine Similarity
    private double calculateKeywordDistance(String document, String rawQuery) {
        if (rawQuery == null || rawQuery.trim().isEmpty() || document == null || document.trim().isEmpty()) {
            return 1.0;
        }

        // Define common stop words to filter out
        Set<String> stopWords = Set.of(
            "what", "is", "the", "are", "and", "or", "for", "to", "in", "of", "a", "an", "on", 
            "with", "about", "can", "you", "your", "we", "our", "us", "i", "my", "me", "he", 
            "she", "they", "them", "it", "its", "rules", "rule", "policy", "policies", 
            "requirement", "requirements", "regulation", "regulations", "guideline", "guidelines",
            "according", "uploaded", "document", "documents", "provided", "say", "says", "does", "tell"
        );

        String[] queryWords = rawQuery.toLowerCase().split("[^a-zA-Z0-9]+");
        String[] docWords = document.toLowerCase().split("[^a-zA-Z0-9]+");

        int matchCount = 0;
        int uniqueQueryWords = 0;

        for (String qWord : queryWords) {
            if (qWord.length() > 1 && !stopWords.contains(qWord)) {
                uniqueQueryWords++;
                boolean matchFound = false;
                for (String dWord : docWords) {
                    if (dWord.equals(qWord) || 
                        (dWord.length() > 3 && qWord.length() > 3 && (dWord.contains(qWord) || qWord.contains(dWord))) ||
                        (dWord.startsWith(qWord) && dWord.length() - qWord.length() <= 5) ||
                        (qWord.startsWith(dWord) && qWord.length() - dWord.length() <= 5)) {
                        matchFound = true;
                        break;
                    }
                }
                if (matchFound) {
                    matchCount++;
                }
            }
        }

        if (uniqueQueryWords == 0) {
            // Fallback: match any query words of length > 1 if all were stop words
            for (String qWord : queryWords) {
                if (qWord.length() > 1) {
                    uniqueQueryWords++;
                    boolean matchFound = false;
                    for (String dWord : docWords) {
                        if (dWord.equals(qWord) || 
                            (dWord.length() > 3 && qWord.length() > 3 && (dWord.contains(qWord) || qWord.contains(dWord))) ||
                            (dWord.startsWith(qWord) && dWord.length() - qWord.length() <= 5) ||
                            (qWord.startsWith(dWord) && qWord.length() - dWord.length() <= 5)) {
                            matchFound = true;
                            break;
                        }
                    }
                    if (matchFound) {
                        matchCount++;
                    }
                }
            }
        }

        if (uniqueQueryWords == 0) {
            return 1.0;
        }

        double similarity = (double) matchCount / uniqueQueryWords;
        return 1.0 - similarity;
    }

    private List<ChromaSearchResult> performInMemoryQuery(List<Double> queryEmbedding,
                                                          int nResults,
                                                          String role,
                                                          String department,
                                                          String rawQuery) {
        List<ChromaSearchResult> matches = new ArrayList<>();

        for (MockChromaRecord record : mockDb) {
            Map<String, Object> meta = record.metadata();
            String roleScope = meta != null ? (String) meta.getOrDefault("role_scope", "ALL") : "ALL";
            String deptScope = meta != null ? (String) meta.getOrDefault("department", "ALL") : "ALL";

            // Role and Department security filters (ADMIN bypasses)
            boolean roleMatch = "ADMIN".equalsIgnoreCase(role) || "ALL".equalsIgnoreCase(roleScope) || roleScope.equalsIgnoreCase(role);
            boolean deptMatch = "ADMIN".equalsIgnoreCase(role) || "ALL".equalsIgnoreCase(deptScope) || deptScope.equalsIgnoreCase(department);

            if (roleMatch && deptMatch) {
                double distance = calculateCosineDistance(queryEmbedding, record.embedding());
                if (rawQuery != null) {
                    distance = calculateKeywordDistance(record.document(), rawQuery);
                    if (distance >= 1.0) {
                        continue;
                    }
                }
                
                Long docId = meta.containsKey("doc_id") ? ((Number) meta.get("doc_id")).longValue() : null;
                String docName = (String) meta.getOrDefault("doc_name", "Unknown");
                String section = (String) meta.getOrDefault("section", "General");

                matches.add(new ChromaSearchResult(
                        record.id(),
                        record.document(),
                        distance,
                        docId,
                        docName,
                        section,
                        roleScope,
                        deptScope
                ));
            }
        }

        // Sort by distance ascending (closest first)
        matches.sort(Comparator.comparingDouble(ChromaSearchResult::distance));

        if (matches.size() > nResults) {
            return matches.subList(0, nResults);
        }
        return matches;
    }

    // Computes cosine distance = 1.0 - cosineSimilarity
    private double calculateCosineDistance(List<Double> vecA, List<Double> vecB) {
        if (vecA == null || vecB == null || vecA.size() != vecB.size() || vecA.isEmpty()) {
            return 1.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            double a = vecA.get(i);
            double b = vecB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 1.0;
        }

        double similarity = dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
        // Bound checks
        similarity = Math.max(-1.0, Math.min(1.0, similarity));
        return 1.0 - similarity;
    }
}

