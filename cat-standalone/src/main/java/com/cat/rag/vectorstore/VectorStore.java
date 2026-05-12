package com.cat.rag.vectorstore;

import java.util.List;
import java.util.Map;

public interface VectorStore {
    void store(String kbId, String chunkId, float[] vector, Map<String, String> metadata);
    List<SearchResult> search(String kbId, float[] queryVector, int topK);
    void deleteByKb(String kbId);
    void deleteByChunkId(String chunkId);
}
