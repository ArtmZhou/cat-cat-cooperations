package com.cat.knowledgebase;

import com.cat.common.exception.BusinessException;
import com.cat.common.model.PageResult;
import com.cat.rag.embedding.EmbeddingService;
import com.cat.rag.vectorstore.SearchResult;
import com.cat.rag.vectorstore.VectorStore;
import com.cat.store.JsonFileStore;
import com.cat.store.entity.StoredDocument;
import com.cat.store.entity.StoredDocumentChunk;
import com.cat.store.entity.StoredKnowledgeBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final JsonFileStore<StoredKnowledgeBase> kbStore;
    private final JsonFileStore<StoredDocument> documentStore;
    private final JsonFileStore<StoredDocumentChunk> chunkStore;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Autowired(required = false)
    private VectorStore vectorStore;

    public StoredKnowledgeBase create(Map<String, Object> input) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        StoredKnowledgeBase kb = new StoredKnowledgeBase();
        kb.setId(id);
        kb.setName((String) input.get("name"));
        kb.setDescription((String) input.get("description"));
        kb.setEmbeddingProvider((String) input.getOrDefault("embeddingProvider", ""));
        kb.setEmbeddingModel((String) input.getOrDefault("embeddingModel", ""));
        kb.setVectorStoreProvider((String) input.getOrDefault("vectorStoreProvider", "lucene"));
        kb.setChunkSize((int) input.getOrDefault("chunkSize", 512));
        kb.setChunkOverlap((int) input.getOrDefault("chunkOverlap", 64));
        kb.setMaxTokens((int) input.getOrDefault("maxTokens", 2000));
        kb.setCreatedAt(now);
        kb.setUpdatedAt(now);

        kbStore.save(id, kb);
        log.info("Knowledge base created: {} ({})", kb.getName(), id);
        return kb;
    }

    public StoredKnowledgeBase getById(String id) {
        return kbStore.findById(id)
            .orElseThrow(() -> new BusinessException(404, "知识库不存在: " + id));
    }

    public PageResult<StoredKnowledgeBase> list(int page, int pageSize) {
        JsonFileStore.PageResult<StoredKnowledgeBase> storeResult = kbStore.findPage(page, pageSize, null);
        return new PageResult<>(
            storeResult.getItems(),
            storeResult.getTotal(),
            storeResult.getPage(),
            storeResult.getPageSize(),
            storeResult.getTotalPages()
        );
    }

    public StoredKnowledgeBase update(String id, Map<String, Object> updates) {
        StoredKnowledgeBase kb = getById(id);

        if (updates.containsKey("name")) kb.setName((String) updates.get("name"));
        if (updates.containsKey("description")) kb.setDescription((String) updates.get("description"));
        if (updates.containsKey("embeddingProvider")) kb.setEmbeddingProvider((String) updates.get("embeddingProvider"));
        if (updates.containsKey("embeddingModel")) kb.setEmbeddingModel((String) updates.get("embeddingModel"));
        if (updates.containsKey("vectorStoreProvider")) kb.setVectorStoreProvider((String) updates.get("vectorStoreProvider"));
        if (updates.containsKey("chunkSize")) kb.setChunkSize((int) updates.get("chunkSize"));
        if (updates.containsKey("chunkOverlap")) kb.setChunkOverlap((int) updates.get("chunkOverlap"));
        if (updates.containsKey("maxTokens")) kb.setMaxTokens((int) updates.get("maxTokens"));
        kb.setUpdatedAt(LocalDateTime.now());

        kbStore.save(id, kb);
        log.info("Knowledge base updated: {}", id);
        return kb;
    }

    public void delete(String id) {
        getById(id);
        chunkStore.delete(c -> c.getKbId().equals(id));
        documentStore.delete(d -> d.getKbId().equals(id));
        vectorStore.deleteByKb(id);
        kbStore.deleteById(id);
        log.info("Knowledge base deleted (cascade): {}", id);
    }

    public List<SearchResult> search(String kbId, String query, int topK) {
        getById(kbId);
        if (embeddingService == null) {
            throw new BusinessException(400, "嵌入服务未配置，无法检索");
        }
        float[] queryVector = embeddingService.embed(query);
        return vectorStore.search(kbId, queryVector, topK);
    }
}
