# RAG 服务采用接口抽象模式

项目现有架构风格为 Controller → Service → JsonFileStore，不使用接口/实现分离。但 RAG 子系统（嵌入服务和向量存储）需要支持多种 provider 可插拔切换（OpenAI API、本地模型、Lucene、Qdrant 等），因此为 `EmbeddingService` 和 `VectorStore` 定义统一接口，每种 provider 独立实现。

**Considered Options:**
- **接口抽象（选用）**: 定义 `EmbeddingService` 和 `VectorStore` 接口，每种 provider 实现接口，通过配置选择 bean。和项目其余部分的风格不一致，但 provider 可插拔性要求统一契约。
- **条件 Bean 注入（否决）**: 每种 provider 各自独立的 Service bean，用 `@ConditionalOnProperty` 决定注入。和项目风格一致，但缺少统一契约，调用方需要知道具体实现类，新增 provider 时调用方也得改。

选择接口抽象是因为 provider 可插拔是核心需求——调用方只能依赖接口，不可感知具体实现。这是 RAG 子系统内部的一致性，不影响外部 Controller/Service/Store 的扁平风格。
