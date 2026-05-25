package com.test.mall.agent.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 商品相关工具 - 提供给 Agent 调用
 */
@Slf4j
@Component
public class ProductTools {

    private final Map<String, String> mockProducts = new HashMap<>();

    public ProductTools() {
        mockProducts.put("iPhone 15 Pro", "iPhone 15 Pro，256GB，钛金属，价格：8999元，库存：100件");
        mockProducts.put("MacBook Air M3", "MacBook Air M3 13寸，16GB+512GB，价格：10499元，库存：50件");
        mockProducts.put("AirPods Pro 2", "AirPods Pro 第二代，USB-C接口，价格：1899元，库存：200件");
        mockProducts.put("iPad Air 5", "iPad Air 5，M1芯片，256GB，价格：5999元，库存：80件");
    }

    @Tool("根据商品名称查询商品详情，包括价格、库存、规格参数")
    public String queryProduct(@P("商品名称或关键词") String productName) {
        log.info("[工具调用] 查询商品: {}", productName);
        for (Map.Entry<String, String> entry : mockProducts.entrySet()) {
            if (entry.getKey().toLowerCase().contains(productName.toLowerCase()) ||
                productName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return "未找到商品 \"" + productName + "\"，请尝试其他关键词或联系人工客服";
    }

    @Tool("查询商品库存")
    public String queryStock(@P("商品名称") String productName) {
        log.info("[工具调用] 查询库存: {}", productName);
        String product = queryProduct(productName);
        if (product.contains("库存")) {
            int start = product.indexOf("库存：");
            return product.substring(start);
        }
        return "暂无库存信息";
    }

    @Tool("根据用户需求推荐商品，如用途、预算等")
    public String recommendProduct(@P("用户的用途、预算或偏好描述") String requirement) {
        log.info("[工具调用] 商品推荐: {}", requirement);
        if (requirement.contains("手机") || requirement.contains("拍照")) {
            return "推荐：iPhone 15 Pro（8999元）- 专业级拍照，A17 Pro芯片，钛金属机身";
        }
        if (requirement.contains("电脑") || requirement.contains("办公")) {
            return "推荐：MacBook Air M3（10499元）- 轻薄便携，18小时续航，适合办公学习";
        }
        if (requirement.contains("耳机") || requirement.contains("降噪")) {
            return "推荐：AirPods Pro 2（1899元）- 主动降噪，空间音频，USB-C充电";
        }
        return "根据您的需求，推荐浏览我们的热门商品：iPhone 15 Pro、MacBook Air M3、AirPods Pro 2";
    }
}
