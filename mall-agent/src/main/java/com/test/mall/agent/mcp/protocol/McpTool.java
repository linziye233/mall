package com.test.mall.agent.mcp.protocol;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * MCP 工具定义 - 符合 Model Context Protocol 标准
 */
@Data
@Builder
public class McpTool {

    private String name;
    private String description;
    private McpInputSchema inputSchema;

    @Data
    @Builder
    public static class McpInputSchema {
        private String type;
        private Map<String, McpProperty> properties;
        private List<String> required;
    }

    @Data
    @Builder
    public static class McpProperty {
        private String type;
        private String description;
    }
}
