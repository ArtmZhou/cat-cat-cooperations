package com.cat.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StoredKnowledgeBase {
    private String id;
    private String name;
    private String description;
    private String embeddingProvider;
    private String embeddingModel;
    private String vectorStoreProvider;
    private int chunkSize = 512;
    private int chunkOverlap = 64;
    private int maxTokens = 2000;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
