# mall-agent：AI 智能客服 Agent 服务

基于 **LangChain4j** 的多 Agent 协作智能客服系统，为电商平台提供 AI 驱动的客户服务能力。


---

## 核心亮点

### 1. 多 Agent 协作编排（Supervisor 模式）

```
用户请求 → Supervisor Agent → 意图识别 → 路由分发
                              ↓
        ┌─────────┬──────────┼──────────┐
        ▼         ▼          ▼          ▼
    ┌───────┐ ┌───────┐ ┌───────┐ ┌───────┐
    │ 订单  │ │ 商品  │ │ 售后  │ │ 通用  │
    │ Agent │ │ Agent │ │ Agent │ │ RAG   │
    └───────┘ └───────┘ └───────┘ └───────┘
```

- **双层意图识别**：关键词快速匹配 + LLM 兜底分类
- **专业 Agent 分工**：订单、商品、售后各自独立，互不影响
- **LangChain4j AiServices**：声明式 Agent 定义，自动 Tool Calling

### 2. RAG 知识库检索（Redis 向量库）

- **本地 Embedding 模型**：`all-MiniLM-L6-v2` ONNX 运行时，零外部依赖
- **Redis 向量存储**：基于 RediSearch 的向量相似度检索
- **文档自动切分**：`DocumentSplitters.recursive(200, 20)` 智能分段
- **检索增强生成**：Top-5 相关知识片段 + 最小相似度阈值 0.6

### 3. Guardrails 安全防护体系

| 层级 | 防护机制 | 实现 |
|------|---------|------|
| **输入层** | Prompt Injection 检测 | 8 种攻击特征正则匹配 |
| **输入层** | PII 敏感信息识别 | 手机号/身份证/银行卡/邮箱 |
| **输入层** | 输入长度限制 | 最大 2000 字符 |
| **输出层** | 敏感词过滤 | 关键词替换为 `**` |
| **输出层** | 幻觉/低置信度检测 | 检测"不确定/可能/不知道"等标记 |

### 4. SSE 流式输出

- **前端打字机效果**：逐字发送，30ms/字符延迟
- **SseEmitter 超时控制**：120s 长连接
- **错误隔离**：安全校验失败单独返回，不影响连接

---

### 5. MCP 协议集成（Model Context Protocol）

**为什么重要**：Anthropic 提出，2026 已成 AI Agent 领域事实标准。解决了"工具与模型强绑定"的问题——换模型不用重写工具。

**实现深度**：
- **MCP Server**：手写 SSE 传输层，完整实现 JSON-RPC 协议（`initialize` / `tools/list` / `tools/call`）
- **自动工具注册**：通过反射扫描 `@Tool` 注解，自动生成 MCP 标准 JSON Schema
- **MCP Client**：支持动态接入外部 MCP Server，将外部工具注入 Agent 工具链

```
┌─────────────────┐      SSE/MCP      ┌─────────────────┐
│  外部 Agent     │ ←──────────────→ │  mall-agent     │
│  (MCP Client)   │   tools/list      │  (MCP Server)   │
│                 │   tools/call      │                 │
└─────────────────┘                   └─────────────────┘
                                              │
                    ┌─────────────────────────┼─────────────────────────┐
                    ▼                         ▼                         ▼
              ┌──────────┐            ┌──────────┐            ┌──────────┐
              │queryOrder│            │queryStock│            │applyRefund│
              └──────────┘            └──────────┘            └──────────┘
```

### 6. Reflexion 自反思机制

**为什么重要**：LLM 会犯错，关键是 Agent 能发现自己的错误并修正。这是从"问答系统"到"智能体"的关键跃迁。

**实现机制**：
- **Judge LLM 评估**：用独立 LLM 对 Agent 输出进行质量评估（置信度 1-10 + 问题清单）
- **多轮重试**：最多 3 次，每次带上历史反思反馈
- **场景化触发**：只对售后/退款/投诉等高风险场景启用（避免 Token 浪费）

```
第1轮：Agent 处理 → Judge 评估（置信度 4/10）→ 记录反思
第2轮：Agent 重试（带上反思）→ Judge 评估（置信度 7/10）→ 记录反思
第3轮：Agent 再试（带上两次反思）→ Judge 评估（置信度 9/10）→ 通过
```

### 7. 持久化记忆 + 用户画像

**为什么重要**：内存记忆重启即丢，持久化记忆让 Agent 真正"认识"用户。这是个性化服务的核心基础设施。

**实现机制**：
- **RedisChatMemory**：实现 LangChain4j `ChatMemory` 接口，对话历史持久化到 Redis，滑动窗口保留 30 条，7 天过期
- **用户画像提取**：对话积累达到 5 条后，异步触发 LLM 分析，提取偏好/预算/品类/痛点
- **画像注入**：Supervisor 路由前将画像拼接到用户消息中，Agent 能感知"用户之前咨询过 iPhone，偏好苹果产品"

```
用户："这款手机怎么样？"
↓
Supervisor 注入画像：
"这款手机怎么样？【用户画像】偏好：苹果产品；预算：8000-12000；最近关注：iPhone 15 Pro"
↓
ProductAgent 回答："iPhone 15 Pro 目前 8999 元，符合您的预算，相比您上次关注的..."
```

---

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.5 | 基础框架 |
| Java | 17 | 运行时（LangChain4j 要求） |
| LangChain4j | 1.0.0-beta2 | AI Agent 核心框架 |
| DeepSeek / OpenAI | - | LLM 大语言模型 |
| Redis + RediSearch | - | 向量存储 + 持久化记忆 |
| Nacos | 2023.0.1.0 | 服务注册发现 |
| OpenFeign | 4.1.0 | 微服务调用 |

---

## 项目结构

```
mall-agent/
├── pom.xml                              # Spring Boot 3.x + Java 17 + LangChain4j
├── README.md                            # 项目介绍 + 面试话术
└── src/main/java/com/test/mall/agent/
    ├── MallAgentApplication.java        # @EnableAsync
    ├── config/
    │   ├── ModelConfig.java             # 本地 Embedding 模型
    │   ├── RagConfig.java               # Redis 向量库 + 知识库初始化
    │   └── AgentConfig.java             # Agent 组装（RedisChatMemory）
    ├── supervisor/
    │   ├── Intent.java                  # 意图枚举
    │   └── SupervisorAgent.java         # 路由 + 画像注入
    ├── agent/
    │   ├── OrderAgent.java              # 订单专业 Agent
    │   ├── ProductAgent.java            # 商品专业 Agent
    │   └── AfterSalesAgent.java         # 售后专业 Agent
    ├── tools/
    │   ├── OrderTools.java              # 订单查询/退款/物流
    │   └── ProductTools.java            # 商品查询/库存/推荐
    ├── mcp/
    │   ├── protocol/McpTool.java        # MCP 工具定义
    │   ├── McpToolRegistry.java         # 反射扫描 @Tool → MCP Schema
    │   ├── McpServerController.java     # SSE 端点 + JSON-RPC 处理
    │   └── client/McpClientRegistry.java # 外部 MCP Server 接入
    ├── reflexion/
    │   ├── EvaluationResult.java        # 评估结果（置信度/问题/建议）
    │   └── ReflexionService.java        # 自反思 + 多轮重试
    ├── memory/
    │   ├── RedisChatMemory.java         # Redis 持久化 ChatMemory
    │   ├── UserProfile.java             # 用户画像实体
    │   └── UserProfileService.java      # 画像提取 + 注入
    ├── guardrails/
    │   └── GuardrailService.java        # 输入输出安全防护
    ├── service/
    │   ├── ChatService.java             # 统一入口（Guardrails + Reflexion）
    │   └── RagService.java              # RAG 知识库
    ├── controller/
    │   └── ChatController.java          # REST + SSE + MCP 管理接口
    └── resources/
        └── application.yml              # 模型/Redis/Nacos 配置
```

---

## 快速开始

### 1. 环境要求

- **JDK 17+**（LangChain4j 1.x 硬性要求）
- **Redis Stack**（支持 RediSearch 向量索引）
- **Nacos** 服务注册中心（可选）
- **DeepSeek API Key**（或 OpenAI 兼容的 API）

### 2. 配置 API Key

```bash
# 环境变量方式（推荐）
export DEEPSEEK_API_KEY="your-api-key-here"

# 或修改 application.yml
langchain4j:
  open-ai:
    chat-model:
      api-key: your-api-key-here
```

### 3. 启动服务

```bash
cd mall-agent
mvn spring-boot:run
```

服务启动后访问：http://localhost:8899

---

## API 接口

### 聊天接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `POST /api/agent/chat` | 普通聊天 | Supervisor 路由 + 画像注入 |
| `GET /api/agent/chat/stream` | SSE 流式 | 打字机效果 |
| `POST /api/agent/chat/rag` | RAG 问答 | 知识库检索增强 |

### MCP 管理接口
| 接口 | 方法 | 说明 |
|------|------|------|
| `GET /api/agent/mcp/tools` | 本服务工具列表 | 查看暴露的 MCP 工具 |
| `POST /api/agent/mcp/connect` | 连接外部 Server | 接入第三方 MCP |
| `GET /api/agent/mcp/external-tools` | 外部工具列表 | 已接入的外部工具 |

### MCP 协议端点（标准协议）
| 接口 | 说明 |
|------|------|
| `GET /mcp/sse` | SSE 连接端点 |
| `POST /mcp/messages` | JSON-RPC 消息处理 |

---
## 扩展方向

1. **A2A 协议**：Google 推出的 Agent-to-Agent 标准，实现跨系统 Agent 协作
2. **多模态支持**：接入图片理解，用户上传商品图片即可识别
3. **意图模型微调**：用业务数据训练专用分类器，替代 LLM 意图分类
4. **对话分析平台**：埋点统计对话转化率、用户满意度、Agent 准确率
