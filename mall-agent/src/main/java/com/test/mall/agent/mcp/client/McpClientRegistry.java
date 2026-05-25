package com.test.mall.agent.mcp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

/**
 * MCP Client 注册表 - 接入外部 MCP Server
 *
 * 能力：
 * 1. 通过 SSE 连接外部 MCP Server
 * 2. 动态发现外部工具并转换为 LangChain4j @Tool
 * 3. 将外部工具注入到 Agent 的工具链中
 */
@Slf4j
@Component
public class McpClientRegistry {

    @Autowired
    private ObjectMapper objectMapper;

    private final List<ExternalMcpServer> externalServers = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * 连接外部 MCP Server
     */
    public void connect(String serverUrl, String serverName) {
        executor.submit(() -> {
            try {
                // 1. 建立 SSE 连接获取 endpoint
                URL url = new URL(serverUrl + "/sse");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);

                String endpoint = null;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            endpoint = line.substring(5).trim();
                            break;
                        }
                    }
                }

                if (endpoint == null) {
                    log.error("MCP Server {} 未返回 endpoint", serverName);
                    return;
                }

                // 2. 初始化连接
                String initEndpoint = endpoint.startsWith("http") ? endpoint : serverUrl + endpoint;
                sendJsonRpc(initEndpoint, "initialize", Map.of());

                // 3. 获取工具列表
                JsonNode toolsResponse = sendJsonRpc(initEndpoint, "tools/list", Map.of());
                List<McpExternalTool> tools = parseTools(toolsResponse, serverName);

                ExternalMcpServer server = new ExternalMcpServer(serverName, initEndpoint, tools);
                externalServers.add(server);

                log.info("MCP Client 连接成功: {}，发现 {} 个外部工具", serverName, tools.size());

            } catch (Exception e) {
                log.error("MCP Client 连接 {} 失败: {}", serverName, e.getMessage());
            }
        });
    }

    /**
     * 获取所有外部工具，可注入到 Agent
     */
    public List<Object> getExternalToolBeans() {
        List<Object> beans = new ArrayList<>();
        for (ExternalMcpServer server : externalServers) {
            for (McpExternalTool tool : server.tools()) {
                beans.add(createToolProxy(server, tool));
            }
        }
        return beans;
    }

    /**
     * 获取外部工具描述（用于调试展示）
     */
    public List<Map<String, String>> listExternalTools() {
        List<Map<String, String>> result = new ArrayList<>();
        for (ExternalMcpServer server : externalServers) {
            for (McpExternalTool tool : server.tools()) {
                Map<String, String> info = new HashMap<>();
                info.put("server", server.name());
                info.put("name", tool.name());
                info.put("description", tool.description());
                result.add(info);
            }
        }
        return result;
    }

    private JsonNode sendJsonRpc(String endpoint, String method, Map<String, Object> params) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String requestBody = objectMapper.writeValueAsString(Map.of(
                "jsonrpc", "2.0",
                "id", UUID.randomUUID().toString(),
                "method", method,
                "params", params
        ));

        try (OutputStream os = conn.getOutputStream()) {
            os.write(requestBody.getBytes());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return objectMapper.readTree(response.toString());
        }
    }

    private List<McpExternalTool> parseTools(JsonNode response, String serverName) {
        List<McpExternalTool> tools = new ArrayList<>();
        JsonNode result = response.get("result");
        if (result == null || !result.has("tools")) return tools;

        JsonNode toolsNode = result.get("tools");
        for (JsonNode toolNode : toolsNode) {
            tools.add(new McpExternalTool(
                    serverName + "_" + toolNode.get("name").asText(),
                    toolNode.get("description").asText(),
                    toolNode.get("inputSchema")
            ));
        }
        return tools;
    }

    /**
     * 为外部 MCP 工具创建动态代理，包装为 LangChain4j 可用的对象
     */
    private Object createToolProxy(ExternalMcpServer server, McpExternalTool tool) {
        return new ExternalToolProxy(server.endpoint(), tool, objectMapper);
    }

    private record ExternalMcpServer(String name, String endpoint, List<McpExternalTool> tools) {}
    private record McpExternalTool(String name, String description, JsonNode inputSchema) {}

    /**
     * 外部工具代理 - 将 MCP 工具调用转换为 HTTP 请求
     */
    public static class ExternalToolProxy {

        private final String endpoint;
        private final McpExternalTool tool;
        private final ObjectMapper objectMapper;

        public ExternalToolProxy(String endpoint, McpExternalTool tool, ObjectMapper objectMapper) {
            this.endpoint = endpoint;
            this.tool = tool;
            this.objectMapper = objectMapper;
        }

        @Tool("【外部MCP工具】{description}")
        public String call(@P("调用参数(JSON格式)") String arguments) throws Exception {
            Map<String, Object> params = new HashMap<>();
            params.put("name", tool.name.substring(tool.name.indexOf("_") + 1));
            params.put("arguments", objectMapper.readTree(arguments));

            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String requestBody = objectMapper.writeValueAsString(Map.of(
                    "jsonrpc", "2.0",
                    "id", UUID.randomUUID().toString(),
                    "method", "tools/call",
                    "params", params
            ));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(requestBody.getBytes());
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                JsonNode result = objectMapper.readTree(response.toString()).get("result");
                return result != null ? result.get("content").asText() : "调用失败";
            }
        }
    }
}
