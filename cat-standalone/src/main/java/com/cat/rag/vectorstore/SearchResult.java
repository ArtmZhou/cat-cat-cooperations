package com.cat.rag.vectorstore;

import lombok.Data;
import java.util.Map;

@Data
public class SearchResult {
    private String chunkId;
    private String docId;
    private String kbId;
    private String content;
    private Map<String, String> metadata;
    private double score;
}
