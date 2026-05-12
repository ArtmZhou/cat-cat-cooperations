package com.cat.rag.embedding;

import com.cat.rag.RagProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "cat.rag.embedding.provider", havingValue = "openai")
public class OpenAiEmbeddingService implements EmbeddingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final RagProperties properties;

    public OpenAiEmbeddingService(RagProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        RagProperties.Embedding.OpenAi cfg = properties.getEmbedding().getOpenai();

        try {
            Map<String, Object> body = Map.of(
                "input", texts,
                "model", cfg.getModel()
            );
            String json = objectMapper.writeValueAsString(body);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(cfg.getApiKey());

            ResponseEntity<String> response = restTemplate.exchange(
                cfg.getBaseUrl() + "/v1/embeddings",
                HttpMethod.POST,
                new HttpEntity<>(json, headers),
                String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode data = root.get("data");
            List<float[]> results = new ArrayList<>();
            for (int i = 0; i < data.size(); i++) {
                JsonNode embedding = data.get(i).get("embedding");
                float[] vector = new float[embedding.size()];
                for (int j = 0; j < embedding.size(); j++) {
                    vector[j] = (float) embedding.get(j).asDouble();
                }
                results.add(vector);
            }
            return results;
        } catch (Exception e) {
            log.error("OpenAI embedding failed", e);
            throw new RuntimeException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }
}
