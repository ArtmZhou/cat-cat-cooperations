package com.cat.store.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class StoredDocumentChunk {
    private String id;
    private String docId;
    private String kbId;
    private int chunkIndex;
    private String content;
    private Map<String, String> metadata;
    private LocalDateTime createdAt;
}
