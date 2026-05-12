package com.cat.rag.vectorstore;

import com.cat.rag.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "cat.rag.vector-store.provider", havingValue = "lucene", matchIfMissing = true)
public class LuceneVectorStore implements VectorStore {

    private final Path indexRoot;

    public LuceneVectorStore(RagProperties properties) {
        this.indexRoot = Path.of(properties.getVectorStore().getLucene().getIndexDir());
    }

    @Override
    public void store(String kbId, String chunkId, float[] vector, Map<String, String> metadata) {
        try {
            IndexWriter writer = getWriter(kbId);
            Document doc = new Document();
            doc.add(new StringField("chunkId", chunkId, Field.Store.YES));
            doc.add(new StringField("kbId", kbId, Field.Store.YES));
            doc.add(new KnnFloatVectorField("vector", vector));
            doc.add(new StoredField("vectorSize", vector.length));
            if (metadata != null) {
                doc.add(new StoredField("docId", metadata.getOrDefault("docId", "")));
                doc.add(new StoredField("content", metadata.getOrDefault("content", "")));
                doc.add(new StoredField("metadata", String.join(",", metadata.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .toArray(String[]::new))));
            }
            writer.addDocument(doc);
            writer.commit();
        } catch (IOException e) {
            log.error("Failed to store chunk {} in kb {}", chunkId, kbId, e);
            throw new RuntimeException("Vector store failed", e);
        }
    }

    @Override
    public List<SearchResult> search(String kbId, float[] queryVector, int topK) {
        try {
            FSDirectory dir = FSDirectory.open(indexRoot.resolve(kbId));
            DirectoryReader reader = DirectoryReader.open(dir);
            IndexSearcher searcher = new IndexSearcher(reader);
            Query knnQuery = new KnnFloatVectorQuery("vector", queryVector, topK);
            TopDocs topDocs = searcher.search(knnQuery, topK);
            List<SearchResult> results = new ArrayList<>();
            for (ScoreDoc sd : topDocs.scoreDocs) {
                Document doc = searcher.doc(sd.doc);
                SearchResult result = new SearchResult();
                result.setChunkId(doc.get("chunkId"));
                result.setKbId(doc.get("kbId"));
                result.setContent(doc.get("content"));
                String storedMeta = doc.get("metadata");
                if (storedMeta != null && !storedMeta.isEmpty()) {
                    Map<String, String> meta = new HashMap<>();
                    for (String pair : storedMeta.split(",")) {
                        String[] kv = pair.split("=", 2);
                        if (kv.length == 2) meta.put(kv[0], kv[1]);
                    }
                    result.setDocId(meta.getOrDefault("docId", ""));
                    result.setMetadata(meta);
                }
                result.setScore(sd.score);
                results.add(result);
            }
            reader.close();
            return results;
        } catch (IOException e) {
            log.error("Failed to search in kb {}", kbId, e);
            return Collections.emptyList();
        }
    }

    @Override
    public void deleteByKb(String kbId) {
        try {
            IndexWriter writer = getWriter(kbId);
            writer.deleteAll();
            writer.commit();
            log.info("Deleted all vectors for kb {}", kbId);
        } catch (IOException e) {
            log.error("Failed to delete vectors for kb {}", kbId, e);
        }
    }

    @Override
    public void deleteByChunkId(String chunkId) {
        // In Lucene, we'd need to search for documents with this chunkId and delete them.
        // For simplicity, we store chunkId in the document so Term deletion works.
        // We need the kbId though. Let this be a best-effort; the caller should pass kbId too.
        // For now, we mostly use deleteByKb.
    }

    private IndexWriter getWriter(String kbId) throws IOException {
        FSDirectory dir = FSDirectory.open(indexRoot.resolve(kbId));
        IndexWriterConfig config = new IndexWriterConfig();
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return new IndexWriter(dir, config);
    }
}
