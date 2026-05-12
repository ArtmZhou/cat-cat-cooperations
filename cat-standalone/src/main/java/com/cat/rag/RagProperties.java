package com.cat.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cat.rag")
public class RagProperties {
    private Embedding embedding = new Embedding();
    private VectorStoreCfg vectorStore = new VectorStoreCfg();

    @Data
    public static class Embedding {
        private String provider = "";
        private OpenAi openai = new OpenAi();

        @Data
        public static class OpenAi {
            private String apiKey = "";
            private String model = "text-embedding-3-small";
            private String baseUrl = "https://api.openai.com";
        }
    }

    @Data
    public static class VectorStoreCfg {
        private String provider = "lucene";
        private Lucene lucene = new Lucene();
        private Qdrant qdrant = new Qdrant();

        @Data
        public static class Lucene {
            private String indexDir = "./data/vector-index";
        }

        @Data
        public static class Qdrant {
            private String host = "localhost";
            private int port = 6334;
            private String collectionPrefix = "cat-kb-";
        }
    }
}
