package com.test.mall.agent.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP Server SSE 端点 - 实现 Model Context Protocol 标准
 *
 * 协议流程：
 * 1. Client GET /mcp/sse → Server 返回 endpoint URL
 * 2. Client POST /mcp/messages 发送 JSON-RPC 请求
 * 3. Server 通过 SSE 推送响应
 *
 * 支持方法：initialize, tools/list, tools/call
 */
@Slf4j
@RestController
@RequestMapping("/mcp")
public class McpServerController {

    @Autowired
    private McpToolRegistry toolRegistry;

    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, SseEmitter> sessions = new ConcurrentHashMap<>();

    /**
     * SSE 连接端点 - Client 首先连接这里
     */
    @GetMapping(value = "/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sse() {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));
        emitter.onError(e -> sessions.remove(sessionId));

        try {
            // 发送 endpoint 事件，告知 Client 后续的 POST 地址
            emitter.send(SseEmitter.event()
                    .name("endpoint")
                    .data("/mcp/messages?sessionId=" + sessionId));
            sessions.put(sessionId, emitter);
            log.info("MCP 会话建立: {}", sessionId);
        } catch (IOException e) {
            log.error("MCP SSE 发送失败", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    /**
     * JSON-RPC 消息处理端点
     */
    @PostMapping("/messages")
    public void messages(@RequestParam String sessionId, @RequestBody JsonNode request) {
        log.debug("MCP 收到请求: session={}, method={}", sessionId, request.get("method"));

        String method = request.has("method") ? request.get("method").asText() : "";
        JsonNode id = request.get("id");

        JsonNode result;
        try {
            result = switch (method) {
                case "initialize" -> handleInitialize();
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolCall(request.get("params"));
                default -> errorResponse(-32601, "Method not found: " + method);
            };
        } catch (Exception e) {
            log.error("MCP 处理异常", e);
            result = errorResponse(-32603, "Internal error: " + e.getMessage());
        }

        sendResponse(sessionId, id, result);
    }

    private JsonNode handleInitialize() {
        ObjectNode result = objectMapper.createObjectNode();
        result.put("protocolVersion", "2024-11-05");
        ObjectNode capabilities = result.putObject("capabilities");
        capabilities.putObject("tools");
        ObjectNode serverInfo = result.putObject("serverInfo");
        serverInfo.put("name", "mall-agent-mcp-server");
        serverInfo.put("version", "1.0.0");
        return result;
    }

    private JsonNode handleToolsList() {
        ArrayNode tools = objectMapper.createArrayNode();
        toolRegistry.listTools().forEach(tool -> {
            ObjectNode toolNode = tools.addObject();
            toolNode.put("name", tool.getName());
            toolNode.put("description", tool.getDescription());
            toolNode.set("inputSchema", objectMapper.valueToTree(tool.getInputSchema()));
        });

        ObjectNode result = objectMapper.createObjectNode();
        result.set("tools", tools);
        return result;
    }

    private JsonNode handleToolCall(JsonNode params) {
        String toolName = params.get("name").asText();
        JsonNode arguments = params.get("arguments");

        Map<String, Object> args = objectMapper.convertValue(arguments, Map.class);

        try {
            String output = toolRegistry.callTool(toolName, args);
            ObjectNode result = objectMapper.createObjectNode();
            result.put("content", output);
            result.put("isError", false);
            return result;
        } catch (Exception e) {
            ObjectNode result = objectMapper.createObjectNode();
            result.put("content", "工具调用失败: " + e.getMessage());
            result.put("isError", true);
            return result;
        }
    }

    private JsonNode errorResponse(int code, String message) {
        ObjectNode error = objectMapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        return error;
    }

    private void sendResponse(String sessionId, JsonNode id, JsonNode result) {
        SseEmitter emitter = sessions.get(sessionId);
        if (emitter == null) {
            log.warn("MCP 会话不存在: {}", sessionId);
            return;
        }

        try {
            ObjectNode response = objectMapper.createObjectNode();
            response.put("jsonrpc", "2.0");
            if (id != null && !id.isNull()) {
                response.set("id", id);
            }
            response.set("result", result);

            emitter.send(SseEmitter.event().data(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.error("MCP 响应发送失败", e);
        }
    }
}
