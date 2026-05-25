package com.test.mall.agent.mcp;

import com.test.mall.agent.mcp.protocol.McpTool;
import com.test.mall.agent.tools.OrderTools;
import com.test.mall.agent.tools.ProductTools;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;

/**
 * MCP 工具注册表 - 将 @Tool 注解的方法注册为 MCP 标准工具
 *
 * 核心能力：
 * 1. 自动扫描 Spring 容器中的 @Tool 组件
 * 2. 反射解析方法签名，生成 MCP 标准工具描述（JSON Schema）
 * 3. 通过方法名路由调用
 */
@Slf4j
@Component
public class McpToolRegistry {

    @Autowired
    private OrderTools orderTools;

    @Autowired
    private ProductTools productTools;

    private final Map<String, ToolEntry> toolMap = new HashMap<>();

    @PostConstruct
    public void init() {
        // 扫描 OrderTools 中的所有 @Tool 方法
        scanTools(orderTools, "order_");
        // 扫描 ProductTools 中的所有 @Tool 方法
        scanTools(productTools, "product_");

        log.info("MCP 工具注册完成，共 {} 个工具", toolMap.size());
    }

    /**
     * 反射扫描 @Tool 方法并注册
     */
    private void scanTools(Object toolBean, String prefix) {
        for (Method method : toolBean.getClass().getDeclaredMethods()) {
            Tool toolAnno = method.getAnnotation(Tool.class);
            if (toolAnno == null) continue;

            String toolName = prefix + method.getName();
            McpTool mcpTool = buildMcpTool(toolName, toolAnno.value(), method);
            toolMap.put(toolName, new ToolEntry(toolBean, method, mcpTool));
        }
    }

    /**
     * 构建 MCP 标准工具描述（JSON Schema 格式）
     */
    private McpTool buildMcpTool(String name, String description, Method method) {
        Map<String, McpTool.McpProperty> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        for (Parameter param : method.getParameters()) {
            P pAnno = param.getAnnotation(P.class);
            if (pAnno == null) continue; // 跳过非业务参数

            properties.put(param.getName(), McpTool.McpProperty.builder()
                    .type(mapJavaTypeToJsonType(param.getType()))
                    .description(pAnno.value())
                    .build());
            required.add(param.getName());
        }

        return McpTool.builder()
                .name(name)
                .description(description)
                .inputSchema(McpTool.McpInputSchema.builder()
                        .type("object")
                        .properties(properties)
                        .required(required)
                        .build())
                .build();
    }

    private String mapJavaTypeToJsonType(Class<?> type) {
        if (type == String.class) return "string";
        if (type == int.class || type == Integer.class) return "integer";
        if (type == double.class || type == Double.class) return "number";
        if (type == boolean.class || type == Boolean.class) return "boolean";
        return "string";
    }

    public List<McpTool> listTools() {
        return toolMap.values().stream()
                .map(ToolEntry::mcpTool)
                .toList();
    }

    /**
     * 调用指定工具
     */
    public String callTool(String toolName, Map<String, Object> arguments) throws Exception {
        ToolEntry entry = toolMap.get(toolName);
        if (entry == null) {
            throw new IllegalArgumentException("未知工具: " + toolName);
        }

        Method method = entry.method();
        Object[] params = new Object[method.getParameterCount()];

        int idx = 0;
        for (Parameter param : method.getParameters()) {
            if (param.getAnnotation(P.class) == null) {
                params[idx++] = null;
                continue;
            }
            Object value = arguments.get(param.getName());
            params[idx++] = convertType(value, param.getType());
        }

        Object result = method.invoke(entry.bean(), params);
        return result != null ? result.toString() : "";
    }

    private Object convertType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType == String.class) return value.toString();
        if (targetType == int.class || targetType == Integer.class) {
            return Integer.parseInt(value.toString());
        }
        return value;
    }

    private record ToolEntry(Object bean, Method method, McpTool mcpTool) {}
}
