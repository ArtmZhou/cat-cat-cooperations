package com.cat.rag.vectorstore;

import com.cat.rag.RagProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Qdrant vector store using REST API.
 * Activate with: cat.rag.vector-store.provider=qdrant
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "cat.rag.vector-store.provider", havingValue = "qdrant")
public class QdrantVectorStore implements VectorStore {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String collectionPrefix;

    public QdrantVectorStore(RagProperties properties, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.baseUrl = "http://localhost:6333";
        this.collectionPrefix = "cat-kb-";
    }

    private String collectionName(String kbId) {
        return collectionPrefix + kbId.replace("-", "");
    }

    @Override
    public void store(String kbId, String chunkId, float[] vector, Map<String, String> metadata) {
        try {
            String collection = collectionName(kbId);
            ensureCollection(collection, vector.length);

            Map<String, Object> point = new LinkedHashMap<>();
            point.put("id", chunkId);
            point.put("vector", vector);
            point.put("payload", metadata != null ? metadata : Map.of());

            Map<String, Object> body = Map.of("points", List.of(point));
            restTemplate.put(baseUrl + "/collections/" + collection + "/points?wait=true",
                new HttpEntity<>(body, jsonHeaders()));
        } catch (Exception e) {
            log.error("Failed to store chunk {} in qdrant kb {}", chunkId, kbId, e);
            throw new RuntimeException("Qdrant store failed", e);
        }
    }

    @Override
    public List<SearchResult> search(String kbId, float[] queryVector, int topK) {
        try {
            String collection = collectionName(kbId);
            Map<String, Object> body = Map.of(
                "vector", queryVector,
                "limit", topK,
                "with_payload", true
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/collections/" + collection + "/points/search",
                new HttpEntity<>(body, jsonHeaders()), String.class);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode result = root.get("result");
            List<SearchResult> results = new ArrayList<>();
            for (JsonNode r : result) {
                SearchResult sr = new SearchResult();
                sr.setChunkId(r.get("id").asText());
                sr.setScore(r.get("score").asDouble());

                JsonNode payload = r.get("payload");
                if (payload != null) {
                    sr.setContent(payload.has("content") ? payload.get("content").asText() : "");
                    sr.setDocId(payload.has("docId") ? payload.get("docId").asText() : "");
                    Map<String, String> meta = new HashMap<>();
                    payload.fields().forEachRemaining(e ->
                        meta.put(e.getKey(), e.getValue().asText()));
                    sr.setMetadata(meta);
                }
                results.add(sr);
            }
            return results;
        } catch (Exception e) {
            log.error("Failed to search in qdrant kb {}", kbId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteByKb(String kbId) {
        try {
            String collection = collectionName(kbId);
            restTemplate.delete(baseUrl + "/collections/" + collection);
            log.info("Deleted qdrant collection {}", collection);
        } catch (Exception e) {
            log.warn("Failed to delete qdrant collection for kb {}", kbId);
        }
    }

    @Override
    public void deleteByChunkId(String chunkId) {
        // Qdrant supports point deletion by ID but we need kbId for collection name.
        // Best-effort: iterate all collections? Not efficient.
        // For cascade delete, use deleteByKb instead.
    }

    private void ensureCollection(String name, int vectorSize) {
        try {
            restTemplate.getForEntity(baseUrl + "/collections/" + name, String.class);
        } catch (Exception e) {
            Map<String, Object> config = Map.of("vectors", Map.of(
                "size", vectorSize,
                "distance", "Cosine"
            ));
            Map<String, Object> body = Map.of(
                "name", name,
                "vectors", config.get("vectors")
            );
            restTemplate.put(baseUrl + "/collections/" + name,
                new HttpEntity<>(Map.of(
                    "vectors", Map.of("size", vectorSize, "distance", "Cosine")
                ), jsonHeaders()));
            log.info("Created qdrant collection {}", name);
        }
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return h;
    }
}
