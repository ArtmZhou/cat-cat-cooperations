package com.cat.rag;

import com.cat.common.exception.BusinessException;
import com.cat.rag.embedding.EmbeddingService;
import com.cat.rag.vectorstore.VectorStore;
import com.cat.store.JsonFileStore;
import com.cat.store.entity.StoredDocument;
import com.cat.store.entity.StoredDocumentChunk;
import com.cat.store.entity.StoredKnowledgeBase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseDocumentService {

    private final JsonFileStore<StoredKnowledgeBase> kbStore;
    private final JsonFileStore<StoredDocument> documentStore;
    private final JsonFileStore<StoredDocumentChunk> chunkStore;
    private final DocumentChunkingService chunkingService;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${cat.data-dir:./data}")
    private String dataDir;

    @Autowired(required = false)
    private EmbeddingService embeddingService;

    @Autowired(required = false)
    private VectorStore vectorStore;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "md", "txt", "pdf", "java", "ts", "vue", "js", "py", "xml", "yaml", "yml", "json"
    );
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_DOCS_PER_KB = 100;
    private static final long MAX_TOTAL_SIZE_PER_KB = 100 * 1024 * 1024;

    public StoredDocument upload(String kbId, MultipartFile file) {
        StoredKnowledgeBase kb = kbStore.findById(kbId)
            .orElseThrow(() -> new BusinessException(404, "知识库不存在: " + kbId));

        String originalName = file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(400, "不支持的文件类型: ." + ext);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小超过10MB限制");
        }

        long docCount = documentStore.count(d -> d.getKbId().equals(kbId));
        if (docCount >= MAX_DOCS_PER_KB) {
            throw new BusinessException(400, "知识库文档数已达上限 (100)");
        }
        long totalSize = documentStore.find(d -> d.getKbId().equals(kbId))
            .stream().mapToLong(StoredDocument::getFileSize).sum();
        if (totalSize + file.getSize() > MAX_TOTAL_SIZE_PER_KB) {
            throw new BusinessException(400, "知识库总大小已达上限 (100MB)");
        }

        String content;
        try {
            if ("pdf".equals(ext)) {
                content = extractPdfText(file);
            } else {
                content = new String(file.getBytes(), "UTF-8");
            }
            saveOriginalFile(kbId, file);
        } catch (IOException e) {
            throw new BusinessException(400, "文件读取失败: " + e.getMessage());
        }

        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        StoredDocument doc = new StoredDocument();
        doc.setId(id);
        doc.setKbId(kbId);
        doc.setFileName(originalName);
        doc.setFileType(ext);
        doc.setFileSize(file.getSize());
        doc.setStatus("CHUNKING");
        doc.setChunkCount(0);
        doc.setEmbeddedChunks(0);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);
        documentStore.save(id, doc);

        List<DocumentChunkingService.Chunk> chunksList = chunkingService.chunk(content, kb.getChunkSize(), kb.getChunkOverlap());
        List<StoredDocumentChunk> chunks = new ArrayList<>();
        for (DocumentChunkingService.Chunk c : chunksList) {
            StoredDocumentChunk chunk = new StoredDocumentChunk();
            chunk.setId(UUID.randomUUID().toString());
            chunk.setDocId(id);
            chunk.setKbId(kbId);
            chunk.setChunkIndex(c.getIndex());
            chunk.setContent(c.getContent());
            Map<String, String> meta = new HashMap<>();
            meta.put("fileName", originalName);
            meta.put("fileType", ext);
            if (c.getHeading() != null && !c.getHeading().isEmpty()) {
                meta.put("heading", c.getHeading());
            }
            chunk.setMetadata(meta);
            chunk.setCreatedAt(now);
            chunks.add(chunk);
            chunkStore.save(chunk.getId(), chunk);
        }

        doc.setChunkCount(chunks.size());
        doc.setStatus("EMBEDDING");
        documentStore.save(id, doc);

        embedAsync(kbId, id, chunks);

        log.info("Document uploaded: {} ({} chunks) to kb {}", originalName, chunks.size(), kbId);
        return doc;
    }

    @Async
    public void embedAsync(String kbId, String docId, List<StoredDocumentChunk> chunks) {
        int total = chunks.size();
        for (int i = 0; i < total; i++) {
            StoredDocumentChunk chunk = chunks.get(i);
            try {
                float[] vector = embeddingService.embed(chunk.getContent());
                Map<String, String> meta = new HashMap<>();
                meta.put("docId", docId);
                meta.put("fileName", chunk.getMetadata() != null ? chunk.getMetadata().getOrDefault("fileName", "") : "");
                meta.put("content", chunk.getContent());
                vectorStore.store(kbId, chunk.getId(), vector, meta);

                StoredDocument doc = documentStore.findById(docId).orElse(null);
                if (doc != null) {
                    doc.setEmbeddedChunks(i + 1);
                    doc.setUpdatedAt(LocalDateTime.now());
                    documentStore.save(docId, doc);
                }

                Map<String, Object> progress = Map.of(
                    "docId", docId,
                    "embedded", i + 1,
                    "total", total,
                    "status", (i + 1 == total) ? "READY" : "EMBEDDING"
                );
                messagingTemplate.convertAndSend("/topic/kb/" + kbId + "/embedding-progress", progress);

                if (i + 1 == total) {
                    doc = documentStore.findById(docId).orElse(null);
                    if (doc != null) {
                        doc.setStatus("READY");
                        doc.setUpdatedAt(LocalDateTime.now());
                        documentStore.save(docId, doc);
                    }
                }
            } catch (Exception e) {
                log.error("Embedding failed for chunk {} of doc {}", chunk.getId(), docId, e);
                StoredDocument doc = documentStore.findById(docId).orElse(null);
                if (doc != null) {
                    doc.setStatus("ERROR");
                    documentStore.save(docId, doc);
                }
                break;
            }
        }
    }

    public List<StoredDocument> listDocuments(String kbId) {
        return documentStore.find(d -> d.getKbId().equals(kbId));
    }

    public StoredDocument getDocument(String docId) {
        return documentStore.findById(docId)
            .orElseThrow(() -> new BusinessException(404, "文档不存在: " + docId));
    }

    public void deleteDocument(String docId) {
        StoredDocument doc = getDocument(docId);
        chunkStore.find(c -> c.getDocId().equals(docId))
            .forEach(c -> vectorStore.deleteByChunkId(c.getId()));
        chunkStore.delete(c -> c.getDocId().equals(docId));
        documentStore.deleteById(docId);
        log.info("Document deleted: {}", docId);
    }

    private String extractPdfText(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private void saveOriginalFile(String kbId, MultipartFile file) throws IOException {
        Path kbDir = Path.of(dataDir, "kb-files", kbId);
        Files.createDirectories(kbDir);
        Path target = kbDir.resolve(file.getOriginalFilename());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
    }

    public java.io.File getOriginalFile(String kbId, String fileName) {
        Path target = Path.of(dataDir, "kb-files", kbId, fileName);
        if (!Files.exists(target)) {
            throw new BusinessException(404, "原始文件不存在: " + fileName);
        }
        return target.toFile();
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
