package com.cat.rag;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class DocumentChunkingService {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.+)$", Pattern.MULTILINE);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```");

    public List<Chunk> chunk(String text, int chunkSize, int chunkOverlap) {
        List<Chunk> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) return chunks;

        List<Section> sections = splitByHeading(text);
        int globalIdx = 0;
        for (Section section : sections) {
            List<String> paras = splitByParagraph(section.text());
            for (String para : paras) {
                if (para.trim().isEmpty()) continue;
                if (para.length() <= chunkSize) {
                    Chunk c = new Chunk();
                    c.setIndex(globalIdx++);
                    c.setContent(para.trim());
                    c.setHeading(section.heading());
                    chunks.add(c);
                } else {
                    int start = 0;
                    while (start < para.length()) {
                        int end = Math.min(start + chunkSize, para.length());
                        if (end < para.length()) {
                            int breakPoint = para.lastIndexOf(' ', end);
                            if (breakPoint > start) end = breakPoint;
                        }
                        Chunk c = new Chunk();
                        c.setIndex(globalIdx++);
                        c.setContent(para.substring(start, end).trim());
                        c.setHeading(section.heading());
                        chunks.add(c);
                        start = end - chunkOverlap;
                        if (start < 0) start = 0;
                        if (start >= para.length()) break;
                    }
                }
            }
        }
        return chunks;
    }

    public List<String> chunkContent(String text, int chunkSize, int chunkOverlap) {
        return chunk(text, chunkSize, chunkOverlap).stream()
            .map(Chunk::getContent)
            .toList();
    }

    private List<Section> splitByHeading(String text) {
        List<Section> sections = new ArrayList<>();
        Matcher m = HEADING_PATTERN.matcher(text);
        int lastEnd = 0;
        String currentHeading = "";
        while (m.find()) {
            if (m.start() > lastEnd) {
                sections.add(new Section(currentHeading, text.substring(lastEnd, m.start())));
            }
            lastEnd = m.end();
            currentHeading = m.group(1) + " " + m.group(2);
        }
        if (lastEnd < text.length()) {
            sections.add(new Section(currentHeading, text.substring(lastEnd)));
        } else if (sections.isEmpty()) {
            sections.add(new Section("", text));
        }
        return sections;
    }

    private List<String> splitByParagraph(String text) {
        return List.of(text.split("\\n{2,}"));
    }

    public record Section(String heading, String text) {}

    public static class Chunk {
        private int index;
        private String content;
        private String heading;

        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getHeading() { return heading; }
        public void setHeading(String heading) { this.heading = heading; }
    }
}
