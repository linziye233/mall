package com.test.mall.agent.controller;

import com.test.mall.agent.mcp.McpToolRegistry;
import com.test.mall.agent.mcp.client.McpClientRegistry;
import com.test.mall.agent.service.ChatService;
import com.test.mall.agent.service.RagService;
import com.test.mall.agent.supervisor.SupervisorAgent;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI Agent 聊天接口 + MCP 管理接口
 *
 * 提供：
 * - POST /api/agent/chat          普通聊天
 * - GET  /api/agent/chat/stream   SSE 流式聊天
 * - POST /api/agent/chat/rag      RAG 知识库问答
 * - GET  /api/agent/mcp/tools     MCP 工具列表
 * - POST /api/agent/mcp/connect   连接外部 MCP Server
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private RagService ragService;

    @Autowired
    private SupervisorAgent supervisorAgent;

    @Autowired
    private McpToolRegistry mcpToolRegistry;

    @Autowired
    private McpClientRegistry mcpClientRegistry;

    /**
     * 普通聊天接口
     */
    @PostMapping("/chat")
    public Result<String> chat(@RequestBody ChatRequest request) {
        String response = chatService.chat(request.getMessage(), request.getSessionId());
        return Result.success(response);
    }

    /**
     * SSE 流式聊天接口 - 打字机效果
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestParam String message,
                                  @RequestParam(required = false, defaultValue = "default") String sessionId) {
        log.info("[SSE] session={}, message={}", sessionId, message);
        SseEmitter emitter = new SseEmitter(120000L);

        emitter.onTimeout(() -> {
            log.warn("[SSE] session={} 连接超时", sessionId);
            emitter.complete();
        });
        emitter.onCompletion(() -> log.info("[SSE] session={} 连接完成", sessionId));
        emitter.onError(e -> log.error("[SSE] session={} 连接异常", sessionId, e));

        new Thread(() -> chatService.streamChat(message, sessionId, emitter)).start();
        return emitter;
    }

    /**
     * RAG 知识库问答接口
     */
    @PostMapping("/chat/rag")
    public Result<String> ragChat(@RequestBody ChatRequest request) {
        String answer = ragService.answer(request.getMessage());
        if (answer == null) {
            return Result.success("未在知识库中找到相关信息，请尝试换个问法或联系人工客服。");
        }
        return Result.success(answer);
    }

    // ========== MCP 管理接口 ==========

    /**
     * 列出本服务暴露的 MCP 工具
     */
    @GetMapping("/mcp/tools")
    public Result<List<Map<String, String>>> listMcpTools() {
        List<Map<String, String>> tools = mcpToolRegistry.listTools().stream()
                .map(t -> Map.of(
                        "name", t.getName(),
                        "description", t.getDescription()))
                .collect(Collectors.toList());
        return Result.success(tools);
    }

    /**
     * 连接外部 MCP Server
     */
    @PostMapping("/mcp/connect")
    public Result<String> connectMcpServer(@RequestBody McpConnectRequest request) {
        mcpClientRegistry.connect(request.getUrl(), request.getName());
        return Result.success("已发起连接外部 MCP Server: " + request.getName());
    }

    /**
     * 列出已连接的外部 MCP 工具
     */
    @GetMapping("/mcp/external-tools")
    public Result<List<Map<String, String>>> listExternalTools() {
        return Result.success(mcpClientRegistry.listExternalTools());
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI Agent 服务运行中");
    }

    // ========== DTO ==========

    @Data
    public static class ChatRequest {
        private String message;
        private String sessionId = "default";
    }

    @Data
    public static class McpConnectRequest {
        private String name;
        private String url;
    }

    @Data
    public static class Result<T> {
        private int code;
        private String message;
        private T data;

        public static <T> Result<T> success(T data) {
            Result<T> result = new Result<>();
            result.setCode(200);
            result.setMessage("success");
            result.setData(data);
            return result;
        }
    }
}
