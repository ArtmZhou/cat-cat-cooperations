package com.cat.store.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class StoredDocument {
    private String id;
    private String kbId;
    private String fileName;
    private String fileType;
    private long fileSize;
    private String status; // PENDING, CHUNKING, EMBEDDING, READY, ERROR
    private int chunkCount;
    private int embeddedChunks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
