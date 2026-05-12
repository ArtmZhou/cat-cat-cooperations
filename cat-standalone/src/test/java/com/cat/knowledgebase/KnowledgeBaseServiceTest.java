package com.cat.knowledgebase;

import com.cat.common.exception.BusinessException;
import com.cat.common.model.PageResult;
import com.cat.rag.vectorstore.VectorStore;
import com.cat.store.JsonFileStore;
import com.cat.store.entity.StoredDocument;
import com.cat.store.entity.StoredDocumentChunk;
import com.cat.store.entity.StoredKnowledgeBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KnowledgeBaseServiceTest {

    private JsonFileStore<StoredKnowledgeBase> kbStore;
    private JsonFileStore<StoredDocument> documentStore;
    private JsonFileStore<StoredDocumentChunk> chunkStore;
    private VectorStore vectorStore;
    private KnowledgeBaseService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        kbStore = new JsonFileStore<>(tempDir.toString(), "test_kbs", StoredKnowledgeBase.class);
        documentStore = new JsonFileStore<>(tempDir.toString(), "test_docs", StoredDocument.class);
        chunkStore = new JsonFileStore<>(tempDir.toString(), "test_chunks", StoredDocumentChunk.class);
        vectorStore = mock(VectorStore.class);
        service = new KnowledgeBaseService(kbStore, documentStore, chunkStore);
        Field vsField = KnowledgeBaseService.class.getDeclaredField("vectorStore");
        vsField.setAccessible(true);
        vsField.set(service, vectorStore);
    }

    @Test
    void createAndGet_shouldPersistAndReturnKnowledgeBase() {
        Map<String, Object> input = Map.of(
            "name", "前端规范",
            "description", "Vue/React 编码规范和最佳实践",
            "embeddingProvider", "openai",
            "embeddingModel", "text-embedding-3-small",
            "vectorStoreProvider", "lucene",
            "chunkSize", 512,
            "chunkOverlap", 64,
            "maxTokens", 2000
        );

        StoredKnowledgeBase created = service.create(input);
        assertNotNull(created.getId());
        assertEquals("前端规范", created.getName());
        assertEquals("Vue/React 编码规范和最佳实践", created.getDescription());
        assertEquals("openai", created.getEmbeddingProvider());
        assertEquals("text-embedding-3-small", created.getEmbeddingModel());
        assertEquals("lucene", created.getVectorStoreProvider());
        assertEquals(512, created.getChunkSize());
        assertEquals(64, created.getChunkOverlap());
        assertEquals(2000, created.getMaxTokens());
        assertNotNull(created.getCreatedAt());
        assertNotNull(created.getUpdatedAt());

        StoredKnowledgeBase fetched = service.getById(created.getId());
        assertEquals(created.getId(), fetched.getId());
        assertEquals("前端规范", fetched.getName());
    }

    @Test
    void getById_shouldThrow404WhenNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.getById("nonexistent"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void list_shouldReturnPaginatedResults() {
        for (int i = 0; i < 5; i++) {
            service.create(Map.of("name", "KB-" + i));
        }

        PageResult<StoredKnowledgeBase> page1 = service.list(1, 3);
        assertEquals(5, page1.getTotal());
        assertEquals(3, page1.getItems().size());
        assertEquals(2, page1.getTotalPages());

        PageResult<StoredKnowledgeBase> page2 = service.list(2, 3);
        assertEquals(2, page2.getItems().size());
    }

    @Test
    void update_shouldModifyFieldsAndPersist() {
        StoredKnowledgeBase created = service.create(Map.of(
            "name", "原始名称",
            "description", "原始描述",
            "chunkSize", 256
        ));

        Map<String, Object> updates = Map.of(
            "name", "新名称",
            "chunkSize", 1024
        );

        StoredKnowledgeBase updated = service.update(created.getId(), updates);
        assertEquals("新名称", updated.getName());
        assertEquals("原始描述", updated.getDescription());
        assertEquals(1024, updated.getChunkSize());

        StoredKnowledgeBase fetched = service.getById(created.getId());
        assertEquals("新名称", fetched.getName());
        assertEquals(1024, fetched.getChunkSize());
    }

    @Test
    void update_shouldThrow404WhenNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.update("nonexistent", Map.of("name", "x")));
        assertEquals(404, ex.getCode());
    }

    @Test
    void delete_shouldRemoveKnowledgeBase() {
        StoredKnowledgeBase created = service.create(Map.of("name", "待删除"));
        assertTrue(kbStore.existsById(created.getId()));

        service.delete(created.getId());
        assertFalse(kbStore.existsById(created.getId()));
    }

    @Test
    void delete_shouldThrow404WhenNotFound() {
        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.delete("nonexistent"));
        assertEquals(404, ex.getCode());
    }
}
