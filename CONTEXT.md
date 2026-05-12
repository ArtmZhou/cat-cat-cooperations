# Cat Agent Platform

多 Agent 协同平台，管理 CLI Agent（Claude Code、OpenCode 等）的生命周期、群组协作和对话上下文。

## Language

### 核心概念

**Agent（CLI Agent）**:
一个由外部 CLI 工具驱动的 AI 助手实例，平台管理其进程、会话和消息。
_Avoid_: Bot, 机器人

**Chat Group（群组）**:
多个 Agent 的对话协作空间，用户在其中通过 @ 提及或广播与 Agent 交互。
_Avoid_: 聊天室, 房间

**Capability（能力）**:
Agent 的领域技能标签，包含能力类型、领域标签和熟练度等级，用于按能力查找 Agent。
_Avoid_: 技能, Skill

**Template（模板）**:
Agent 的创建蓝图，预置 CLI 类型、可执行路径、参数、环境变量、输出格式和 Token 解析规则。
_Avoid_: 预设, Preset

### RAG 知识库

**知识库 (Knowledge Base)**:
用户构建的文档集合，经过分块和嵌入后支持语义检索，Agent 引用它以增强回复质量。
_Avoid_: 文档库, 资料库, 语料库

**文档 (Document)**:
上传到知识库的单个文件（Markdown、PDF、TXT、代码文件等），经解析后切分为多个分块。
_Avoid_: 文件, 附件

**分块 (Chunk)**:
文档被切分后的文本片段，是嵌入和检索的基本单元。切分策略：先按标题/段落语义切分，过长的块再按固定大小二次切分。
_Avoid_: 片段, 切片

**嵌入 (Embedding)**:
将文本分块转为浮点数向量，由嵌入服务完成。语义相近的文本向量距离近。
_Avoid_: 向量化, Vectorization

**嵌入服务 (Embedding Service)**:
提供文本→向量转换的插件化服务，支持多种 provider（OpenAI API、本地模型等），通过统一接口抽象。
_Avoid_: 编码器, Encoder

**向量存储 (Vector Store)**:
存储和检索嵌入向量的引擎，支持多种 provider（Lucene、Qdrant、Milvus 等），通过统一接口抽象。
_Avoid_: 索引库, 向量数据库

**检索 (Retrieval)**:
将用户消息转为查询向量，在向量存储中搜索语义最相近的分块，返回 Top-K 个结果。K 可配置。
_Avoid_: 搜索, 查询

**自动注入 (Auto-injection)**:
每次发消息前，系统自动检索相关分块并拼入 Agent 的 prompt，Agent 无需主动请求。
_Avoid_: 上下文注入, 自动挂载

**Token 预算 (Token Budget)**:
限制知识库片段在 Agent prompt 中占用的 token 数上限，防止溢出上下文窗口。可配置，默认 2000 tokens。
_Avoid_: 上下文限制, Token 配额

**Reranking（重排序）**:
对向量检索召回结果用更精准的模型（如 Cross-Encoder）二次打分排序。第一版不做，预留扩展。
_Avoid_: 重排, 精排

## Relationships

- 一个**知识库**包含多个**文档**，删除知识库时级联删除所有文档、分块和向量数据
- 一个**文档**被切分为多个**分块**，分块序号和元数据（来源文件、标题层级）记录在分块实体中
- 每个**分块**对应一个**嵌入**向量，向量存在向量存储中，分块文本和元数据存在 JsonFileStore 中
- 一个 **Agent** 可绑定多个**知识库**，多对多关系
- 一个**群组**可绑定多个**知识库**，多对多关系
- 群聊时：群组知识库和 Agent 个人知识库**合并检索**，统一按相似度排序取 Top-K
- **Token 预算**优先级：System prompt > 群聊上下文 > 知识库片段
- **嵌入**采用异步模式：文档上传后立即返回，后台分块嵌入，通过 WebSocket 推送进度

## Flagged ambiguities

- "知识库"在本项目中特指 RAG 文档集合，不指代数据库或 Wiki 系统
- "检索"特指语义向量检索，不指数据库查询或全文搜索
