package com.cat.controller;

import com.cat.common.model.ApiResponse;
import com.cat.common.model.PageResult;
import com.cat.knowledgebase.KnowledgeBaseService;
import com.cat.rag.KnowledgeBaseDocumentService;
import com.cat.rag.vectorstore.SearchResult;
import com.cat.store.entity.StoredDocument;
import com.cat.store.entity.StoredKnowledgeBase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Tag(name = "知识库管理", description = "知识库的创建、查询、更新、删除、文档管理、检索")
@RestController
@RequestMapping("/api/v1/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService kbService;
    private final KnowledgeBaseDocumentService documentService;

    @Operation(summary = "创建知识库")
    @PostMapping
    public ApiResponse<StoredKnowledgeBase> create(@RequestBody Map<String, Object> body) {
        return ApiResponse.success(kbService.create(body));
    }

    @Operation(summary = "获取知识库列表")
    @GetMapping
    public ApiResponse<PageResult<StoredKnowledgeBase>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return ApiResponse.success(kbService.list(page, pageSize));
    }

    @Operation(summary = "获取知识库详情")
    @GetMapping("/{kbId}")
    public ApiResponse<StoredKnowledgeBase> getById(@PathVariable String kbId) {
        return ApiResponse.success(kbService.getById(kbId));
    }

    @Operation(summary = "更新知识库")
    @PutMapping("/{kbId}")
    public ApiResponse<StoredKnowledgeBase> update(
            @PathVariable String kbId,
            @RequestBody Map<String, Object> body) {
        return ApiResponse.success(kbService.update(kbId, body));
    }

    @Operation(summary = "删除知识库")
    @DeleteMapping("/{kbId}")
    public ApiResponse<Void> delete(@PathVariable String kbId) {
        kbService.delete(kbId);
        return ApiResponse.success();
    }

    // ---- Document management ----

    @Operation(summary = "上传文档")
    @PostMapping("/{kbId}/documents")
    public ApiResponse<StoredDocument> uploadDocument(
            @PathVariable String kbId,
            @RequestParam("file") MultipartFile file) {
        return ApiResponse.success(documentService.upload(kbId, file));
    }

    @Operation(summary = "获取文档列表")
    @GetMapping("/{kbId}/documents")
    public ApiResponse<List<StoredDocument>> listDocuments(@PathVariable String kbId) {
        return ApiResponse.success(documentService.listDocuments(kbId));
    }

    @Operation(summary = "获取文档详情")
    @GetMapping("/{kbId}/documents/{docId}")
    public ApiResponse<StoredDocument> getDocument(
            @PathVariable String kbId,
            @PathVariable String docId) {
        return ApiResponse.success(documentService.getDocument(docId));
    }

    @Operation(summary = "下载原始文件")
    @GetMapping("/{kbId}/documents/{docId}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable String kbId,
            @PathVariable String docId) throws IOException {
        StoredDocument doc = documentService.getDocument(docId);
        File file = documentService.getOriginalFile(kbId, doc.getFileName());
        FileSystemResource resource = new FileSystemResource(file);
        String encodedName = URLEncoder.encode(doc.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(new InputStreamResource(resource.getInputStream()));
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{kbId}/documents/{docId}")
    public ApiResponse<Void> deleteDocument(
            @PathVariable String kbId,
            @PathVariable String docId) {
        documentService.deleteDocument(docId);
        return ApiResponse.success();
    }

    // ---- Search ----

    @Operation(summary = "检索知识库")
    @PostMapping("/{kbId}/search")
    public ApiResponse<List<SearchResult>> search(
            @PathVariable String kbId,
            @RequestBody Map<String, Object> body) {
        String query = (String) body.get("query");
        int topK = body.containsKey("topK") ? (int) body.get("topK") : 5;
        return ApiResponse.success(kbService.search(kbId, query, topK));
    }
}
