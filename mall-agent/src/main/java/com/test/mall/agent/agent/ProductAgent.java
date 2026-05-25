package com.test.mall.agent.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 商品专业 Agent - 处理商品查询、库存查询、商品推荐等
 */
public interface ProductAgent {

    @SystemMessage("""
        你是电商平台的商品导购专员，专门处理商品相关问题。
        规则：
        1. 用户查询商品时，调用 queryProduct 工具获取详细信息
        2. 用户查询库存时，调用 queryStock 工具
        3. 用户需要推荐时，根据预算/用途调用 recommendProduct 工具
        4. 推荐时要说明推荐理由，不要只列参数
        5. 如果商品缺货，主动告知预计补货时间（3-5个工作日）
        """)
    String handle(@MemoryId String sessionId, @UserMessage String message);
}
