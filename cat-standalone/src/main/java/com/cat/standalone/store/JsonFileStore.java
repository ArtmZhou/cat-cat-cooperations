package com.cat.standalone.store;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * JSON文件存储服务
 *
 * 提供基于JSON文件的持久化存储，支持CRUD操作
 */
@Slf4j
public class JsonFileStore<T> {

    private final File dataFile;
    private final ObjectMapper objectMapper;
    private final Class<T> entityClass;
    private final Map<String, T> cache = new ConcurrentHashMap<>();

    public JsonFileStore(String dataDir, String storeName, Class<T> entityClass) {
        this.entityClass = entityClass;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.dataFile = new File(dir, storeName + ".json");
        load();
    }

    /**
     * 从文件加载数据
     */
    @SuppressWarnings("unchecked")
    private void load() {
        if (dataFile.exists()) {
            try {
                Map<String, Object> data = objectMapper.readValue(dataFile,
                    new TypeReference<Map<String, Object>>() {});
                cache.clear();
                for (Map.Entry<String, Object> entry : data.entrySet()) {
                    T entity = objectMapper.convertValue(entry.getValue(), entityClass);
                    cache.put(entry.getKey(), entity);
                }
                log.debug("Loaded {} records from {}", cache.size(), dataFile.getName());
            } catch (IOException e) {
                log.error("Failed to load data from {}", dataFile.getName(), e);
            }
        }
    }

    /**
     * 保存数据到文件
     */
    private synchronized void save() {
        try {
            objectMapper.writeValue(dataFile, cache);
            log.debug("Saved {} records to {}", cache.size(), dataFile.getName());
        } catch (IOException e) {
            log.error("Failed to save data to {}", dataFile.getName(), e);
        }
    }

    /**
     * 保存实体
     */
    public T save(String id, T entity) {
        cache.put(id, entity);
        save();
        return entity;
    }

    /**
     * 批量保存
     */
    public void saveAll(Map<String, T> entities) {
        cache.putAll(entities);
        save();
    }

    /**
     * 根据ID查找
     */
    public Optional<T> findById(String id) {
        return Optional.ofNullable(cache.get(id));
    }

    /**
     * 查找所有
     */
    public List<T> findAll() {
        return new ArrayList<>(cache.values());
    }

    /**
     * 条件查询
     */
    public List<T> find(Predicate<T> predicate) {
        return cache.values().stream()
            .filter(predicate)
            .collect(Collectors.toList());
    }

    /**
     * 查找第一个匹配的
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
        return cache.values().stream()
            .filter(predicate)
            .findFirst();
    }

    /**
     * 统计数量
     */
    public long count() {
        return cache.size();
    }

    /**
     * 条件统计
     */
    public long count(Predicate<T> predicate) {
        return cache.values().stream()
            .filter(predicate)
            .count();
    }

    /**
     * 检查是否存在
     */
    public boolean existsById(String id) {
        return cache.containsKey(id);
    }

    /**
     * 删除
     */
    public void deleteById(String id) {
        cache.remove(id);
        save();
    }

    /**
     * 条件删除
     */
    public void delete(Predicate<T> predicate) {
        Set<String> toRemove = cache.entrySet().stream()
            .filter(e -> predicate.test(e.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        for (String id : toRemove) {
            cache.remove(id);
        }
        if (!toRemove.isEmpty()) {
            save();
        }
    }

    /**
     * 清空所有
     */
    public void deleteAll() {
        cache.clear();
        save();
    }

    /**
     * 分页查询
     */
    public PageResult<T> findPage(int page, int pageSize, Predicate<T> predicate) {
        List<T> filtered = predicate != null
            ? find(predicate)
            : findAll();

        int total = filtered.size();
        int totalPages = (int) Math.ceil((double) total / pageSize);

        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<T> items = fromIndex < total
            ? filtered.subList(fromIndex, toIndex)
            : Collections.emptyList();

        return new PageResult<>(items, total, page, pageSize, totalPages);
    }

    /**
     * 分页结果
     */
    public static class PageResult<T> {
        private final List<T> items;
        private final long total;
        private final int page;
        private final int pageSize;
        private final int totalPages;

        public PageResult(List<T> items, long total, int page, int pageSize, int totalPages) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = totalPages;
        }

        public List<T> getItems() { return items; }
        public long getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return totalPages; }
    }
}